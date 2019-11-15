/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.cli.utilities

import org.jetbrains.kotlin.cli.bc.K2Native
import org.jetbrains.kotlin.konan.file.File
import java.util.concurrent.*
import kotlinx.cli.*

fun generatePlatformLibraries(args: Array<String>) {
    val argParser = ArgParser("generate-platform", prefixStyle = ArgParser.OPTION_PREFIX_STYLE.JVM)
    val inputDirectoryPath by argParser.option(
            ArgType.String, "input-directory", "i", "Input directory").required()
    val outputDirectoryPath by argParser.option(
            ArgType.String, "output-directory", "o", "Output directory").required()
    val target by argParser.option(
            ArgType.String, "target", "t", "Compilation target").required()
    val saveTemps by argParser.option(
            ArgType.Boolean, "save-temps", "s", "Save temporary files").default(false)
    val cacheDirectoryPath by argParser.option(
            ArgType.String, "cache-directory", "c", "Cache output directory")

    argParser.parse(args)

    val inputDirectory = File(inputDirectoryPath)
    val outputDirectory = File(outputDirectoryPath)
    val cacheDirectory = cacheDirectoryPath?.File()

    if (!inputDirectory.exists) throw Error("input directory doesn't exist")
    if (!outputDirectory.exists) {
        outputDirectory.mkdirs()
    }
    File("${outputDirectoryPath}/../cache").mkdirs()

    generatePlatformLibraries(target, inputDirectory, outputDirectory, saveTemps, cacheDirectory)
}

private class DefFile(val name: String, val depends: MutableList<DefFile>) {
    override fun toString(): String = "$name: [${depends.joinToString(separator = ", ") { it.name }}]"
}

private fun topoSort(defFiles: List<DefFile>): List<DefFile> {
    // Do DFS toposort.
    val markGray = mutableSetOf<DefFile>()
    val markBlack = mutableSetOf<DefFile>()
    val result = mutableListOf<DefFile>()

    fun visit(def: DefFile) {
        if (markBlack.contains(def)) return
        if (markGray.contains(def)) throw Error("$def is part of cycle")
        markGray += def
        def.depends.forEach {
            visit(it)
        }
        markGray -= def
        markBlack += def
        result += def
    }

    var index = 0
    while (markBlack.size < defFiles.size) {
        visit(defFiles[index++])
    }
    return result
}

private fun generatePlatformLibraries(target: String, inputDirectory: File, outputDirectory: File,
                                      saveTemps: Boolean, cacheDirectory: File?) {
    fun buildKlib(def: DefFile) {
        val file = File("$inputDirectory/${def.name}.def")
        File("${outputDirectory.absolutePath}/build-${def.name}").mkdirs()
        val outKlib = "${outputDirectory.absolutePath}/build-${def.name}/${def.name}.klib"
        val args = arrayOf("-o", outKlib,
                "-target", target,
                "-def", file.absolutePath,
                "-compiler-option", "-fmodules-cache-path=$outputDirectory/clangModulesCache",
                "-repo",  "${outputDirectory.absolutePath}",
                "-no-default-libs", "-no-endorsed-libs", "-Xpurge-user-libs",
                *def.depends.flatMap { listOf("-l", "$outputDirectory/${it.name}") }.toTypedArray())
        println("Processing ${def.name}...")
        try {
            invokeInterop("native", args)?.let { K2Native.mainNoExit(it) }
            org.jetbrains.kotlin.cli.klib.main(arrayOf("install", outKlib,
                    "-target", target,
                    "-repository", "${outputDirectory.absolutePath}"
            ))
            if (cacheDirectory != null)
                K2Native.mainNoExit(arrayOf("-p", "dynamic_cache",
                    "-target", target,
                    "-repo", "${outputDirectory.absolutePath}",
                    "-Xadd-cache=${outputDirectory.absolutePath}/${def.name}",
                    "-Xcache-directory=${cacheDirectory.absolutePath}"))
        } finally {
            if (!saveTemps) {
                File("$outputDirectory/build-${def.name}").deleteRecursively()
            }
        }
    }
    if (cacheDirectory != null) {
        val stdlib = inputDirectory.parentFile.parentFile.parentFile.child("klib/common/stdlib")
        println("Caching standard library ${stdlib.absolutePath} to ${cacheDirectory.absolutePath}...")
        K2Native.mainNoExit(arrayOf("-p", "dynamic_cache",
                "-target", target,
                "-Xadd-cache=${stdlib.absolutePath}",
                "-Xcache-directory=${cacheDirectory.absolutePath}"))
    }

    println("Generating platform libraries from $inputDirectory to $outputDirectory for $target")
    // Build dependencies graph.
    val defFiles = mutableMapOf<String, DefFile>()
    val dependsRegex = Regex("^depends = (.*)")
    inputDirectory.listFilesOrEmpty.filter { it.extension == "def" }.forEach { file ->
        val name = file.name.split(".").also { assert(it.size == 2) }[0]
        val def = defFiles.getOrPut(name) {
            DefFile(name, mutableListOf())
        }
        file.forEachLine { line ->
            val match = dependsRegex.matchEntire(line)
            if (match != null) {
                match.groupValues[1].split(" ").forEach { dependency ->
                    def.depends.add(defFiles.getOrPut(dependency) {
                        DefFile(dependency, mutableListOf())
                    })
                }
            }
        }
    }
    val sorted = topoSort(defFiles.values.toList())
    val numCores = Runtime.getRuntime().availableProcessors()
    val executorPool = ThreadPoolExecutor(numCores, numCores,
            10, TimeUnit.SECONDS, ArrayBlockingQueue(1000),
            Executors.defaultThreadFactory(), RejectedExecutionHandler { r, _ ->
        println("Execution rejected: $r")
        throw Error("Must not happen!")
    })
    val built = ConcurrentHashMap(sorted.associateWith { 0 })
    // Now run interop tool on toposorted dependencies.
    sorted.forEach { def ->
        executorPool.execute {
            // A bit ugly, we just block here until all dependencies are built.
            while (def.depends.any { built[it] == 0 }) {
                Thread.sleep(100)
            }
            try {
                if (def.depends.any { built[it] == 2 }) {
                    throw Error("There are failed dependencies for $def")
                }
                buildKlib(def)
                built[def] = 1
            } catch(e: Throwable) {
                built[def] = 2
                throw e
            }
        }
    }
    executorPool.shutdown()
    if (!saveTemps) {
        File("${outputDirectory.absolutePath}/clangModulesCache").deleteRecursively()
    }
}