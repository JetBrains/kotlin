/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.cli.utilities

import org.jetbrains.kotlin.cli.bc.K2Native
import org.jetbrains.kotlin.konan.file.File
import java.util.concurrent.*
import kotlinx.cli.*
import org.jetbrains.kotlin.backend.konan.CachedLibraries
import org.jetbrains.kotlin.backend.konan.OutputFiles
import org.jetbrains.kotlin.backend.konan.files.renameAtomic
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.target.customerDistribution
import org.jetbrains.kotlin.konan.util.visibleName
import java.io.PrintWriter
import java.io.StringWriter
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.exitProcess
import java.io.File as JFile

// TODO: We definitely need to unify logging in different parts of the compiler.
private class Logger(val level: Level = Level.NORMAL) {

    fun log(message: String) {
        println(message)
    }

    fun verbose(message: String) {
        if (level == Level.VERBOSE) {
            println(message)
        }
    }

    enum class Level {
        NORMAL, VERBOSE
    }
}

private fun Logger.logFailedLibraries(built: Map<DefFile, ProcessingStatus>) {
    log("Processing platform libraries finished with errors.")
    built.forEach { (def, status) ->
        if (status is ProcessingStatus.FAIL) {
            log("    ${def.name}: ${status.error}")
        }
    }
}

private fun Logger.logStackTrace(error: Throwable) {
    val stringWriter = StringWriter()
    error.printStackTrace(PrintWriter(stringWriter))
    verbose(stringWriter.toString())
}

private fun createTempDir(prefix: String, parent: File): File =
        File(createTempDir(prefix, directory = JFile(parent.absolutePath)).absolutePath)

private sealed class ProcessingStatus {
    object WAIT: ProcessingStatus()
    object SUCCESS: ProcessingStatus()
    object FAILED_DEPENDENCIES: ProcessingStatus()
    class FAIL(val error: Throwable) : ProcessingStatus()
}

// TODO: Use Distribution's paths after compiler update.
fun generatePlatformLibraries(args: Array<String>) {
    // IMPORTANT! These command line keys are used by the Gradle plugin to configure platform libraries generation,
    // so any changes in them must be reflected at the Gradle plugin side too.
    // See org.jetbrains.kotlin.gradle.targets.native.internal.PlatformLibrariesGenerator in the Big Kotlin repo.
    val argParser = ArgParser("generate-platform", prefixStyle = ArgParser.OptionPrefixStyle.JVM)
    val inputDirectoryPath by argParser.option(
            ArgType.String,
            "input-directory", "i",
            "Input directory. Default value is <dist>/konan/platformDef/<target_family>"
    )
    val outputDirectoryPath by argParser.option(
            ArgType.String,
            "output-directory", "o",
            "Output directory. Default value is <dist>/klib/platform/<target>"
    )
    val targetName by argParser.option(
            ArgType.String, "target", "t", "Compilation target").required()
    val saveTemps by argParser.option(
            ArgType.Boolean, "save-temps", "s", "Save temporary files").default(false)
    val stdlibPath by argParser.option(
            ArgType.String,
            "stdlib-path", "S",
            "Place where stdlib is located. Default value is <dist>/klib/common/stdlib"
    )

    val dynamicCacheKind = CompilerOutputKind.DYNAMIC_CACHE.visibleName
    val staticCacheKind = CompilerOutputKind.STATIC_CACHE.visibleName
    val cacheKind by argParser.option(
            ArgType.Choice(listOf(dynamicCacheKind, staticCacheKind)), "cache-kind", "k", "Type of cache."
    ).default(dynamicCacheKind)

    val cacheDirectoryPath by argParser.option(
            ArgType.String, "cache-directory", "c", "Cache output directory")

    val verbose by argParser.option(
            ArgType.Boolean,
            "verbose", "v",
            "Show verbose log messages"
    ).default(false)

    val cacheArgs by argParser.option(
            ArgType.String, "cache-arg",
            description = "An argument passed to compiler during cache building. Used only if -cache-directory is specified."
    ).multiple()

    argParser.parse(args)

    val distribution = customerDistribution()
    val target = HostManager(distribution).targetByName(targetName)

    val inputDirectory = inputDirectoryPath?.File()
            ?: File(distribution.konanSubdir, "platformDef").child(target.family.visibleName)

    val outputDirectory = outputDirectoryPath?.File()
            ?: File(distribution.klib, "platform").child(target.visibleName)

    val cacheDirectory = cacheDirectoryPath?.File()

    if (!inputDirectory.exists) throw Error("input directory doesn't exist")
    if (!outputDirectory.exists) {
        outputDirectory.mkdirs()
    }
    if (cacheDirectory != null && !cacheDirectory.exists) {
        cacheDirectory.mkdirs()
    }

    val stdlibFile = stdlibPath?.File() ?: File(distribution.stdlib)

    val logger = Logger(if (verbose) Logger.Level.VERBOSE else Logger.Level.NORMAL)

    generatePlatformLibraries(
            target, inputDirectory, outputDirectory,
            saveTemps, cacheDirectory, stdlibFile,
            cacheKind, cacheArgs, logger
    )
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

private fun generateLibrary(
        target: KonanTarget,
        def: DefFile,
        inputDirectory: File,
        outputDirectory: File,
        tmpDirectory: File,
        logger: Logger
) {
    val defFile = inputDirectory.child("${def.name}.def")
    val outKlib = outputDirectory.child(def.name)

    if (outKlib.exists) {
        logger.verbose("Skip generating ${def.name} as it's already generated")
        return
    }

    val tmpKlib = tmpDirectory.child(def.name)

    try {
        val cinteropArgs = arrayOf(
                "-o", tmpKlib.absolutePath,
                "-target", target.visibleName,
                "-def", defFile.absolutePath,
                "-compiler-option", "-fmodules-cache-path=${tmpDirectory.child("clangModulesCache").absolutePath}",
                "-repo", outputDirectory.absolutePath,
                "-no-default-libs", "-no-endorsed-libs", "-Xpurge-user-libs", "-nopack",
                *def.depends.flatMap { listOf("-l", "$outputDirectory/${it.name}") }.toTypedArray()
        )
        logger.verbose("Run cinterop with args: ${cinteropArgs.joinToString(separator = " ")}")
        invokeInterop("native", cinteropArgs)?.let { K2Native.mainNoExit(it) }

        // Atomically move the generated library to the destination path.
        if (!renameAtomic(tmpKlib.absolutePath, outKlib.absolutePath, replaceExisting = false)) {
            tmpKlib.deleteRecursively()
        }
    } finally {
        tmpKlib.deleteRecursively()
    }
}

private fun getCacheFile(
        libraryName: String,
        target: KonanTarget,
        cacheDirectory: File,
        cacheKind: String
): File {
    val cacheBaseName = CachedLibraries.getCachedLibraryName(libraryName)
    val cacheOutputKind = CompilerOutputKind.valueOf(cacheKind.toUpperCase())
    return OutputFiles(cacheDirectory.child(cacheBaseName).absolutePath, target, cacheOutputKind).mainFile.File()
}

private fun buildCache(
        target: KonanTarget,
        def: DefFile,
        outputDirectory: File,
        cacheDirectory: File,
        cacheKind: String,
        cacheArgs: List<String>,
        logger: Logger
) {
    val cacheFile = getCacheFile(def.name, target, cacheDirectory, cacheKind)
    if (cacheFile.exists) {
        logger.verbose("Skip precompiling ${def.name} as it's already precompiled")
        return
    }

    val compilerArgs = arrayOf(
            "-p", cacheKind,
            "-target", target.visibleName,
            "-repo", outputDirectory.absolutePath,
            "-Xadd-cache=${outputDirectory.absolutePath}/${def.name}",
            "-Xcache-directory=${cacheDirectory.absolutePath}",
            *cacheArgs.toTypedArray()
    )
    logger.verbose("Run compiler with args: ${compilerArgs.joinToString(separator = " ")}")
    K2Native.mainNoExit(compilerArgs)
}

private fun buildStdlibCache(
        target: KonanTarget,
        stdlib: File,
        cacheDirectory: File,
        cacheKind: String,
        cacheArgs: List<String>,
        logger: Logger
) {
    val stdlibCacheFile = getCacheFile("stdlib", target, cacheDirectory, cacheKind)
    if (stdlibCacheFile.exists) {
        logger.verbose("Skip precompiling standard library as it's already precompiled")
        return
    }

    logger.log("Precompiling standard library...")
    val compilerArgs = arrayOf(
            "-p", cacheKind,
            "-target", target.visibleName,
            "-Xadd-cache=${stdlib.absolutePath}",
            "-Xcache-directory=${cacheDirectory.absolutePath}",
            *cacheArgs.toTypedArray()
    )
    logger.verbose("Run compiler with args: ${compilerArgs.joinToString(separator = " ")}")
    K2Native.mainNoExit(compilerArgs)
}

private fun generatePlatformLibraries(target: KonanTarget, inputDirectory: File, outputDirectory: File,
                                      saveTemps: Boolean, cacheDirectory: File?, stdlibFile: File,
                                      cacheKind: String, cacheArgs: List<String>,
                                      logger: Logger) {
    if (cacheDirectory != null) {
        buildStdlibCache(target, stdlibFile, cacheDirectory, cacheKind, cacheArgs, logger)
    }

    logger.verbose("Generating platform libraries from $inputDirectory to $outputDirectory for ${target.visibleName}")
    if (cacheDirectory != null) {
        logger.verbose("Precompiling platform libraries to $cacheDirectory (cache kind: $cacheKind)")
    }

    val tmpDirectory = createTempDir("build-", outputDirectory)
    // Delete the tmp directory in case of execution interruption.
    val deleteTmpHook = Thread {
        if (!saveTemps) {
            tmpDirectory.deleteRecursively()
        }
    }
    Runtime.getRuntime().addShutdownHook(deleteTmpHook)

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
        logger.log("Execution rejected: $r")
        throw Error("Must not happen!")
    })
    val built = ConcurrentHashMap(sorted.associateWith<DefFile, ProcessingStatus> { ProcessingStatus.WAIT })
    // Now run interop tool on toposorted dependencies.
    val countTotal = sorted.size
    val countProcessed = AtomicInteger(0)
    try {
        tmpDirectory.mkdirs()
        sorted.forEach { def ->
            executorPool.execute {
                // A bit ugly, we just block here until all dependencies are built.
                while (def.depends.any { built[it] == ProcessingStatus.WAIT }) {
                    Thread.sleep(100)
                }
                try {
                    if (def.depends.any { built[it] is ProcessingStatus.FAIL }) {
                        built[def] = ProcessingStatus.FAILED_DEPENDENCIES
                        return@execute
                    }

                    logger.log("Processing ${def.name} (${countProcessed.incrementAndGet()}/$countTotal)...")
                    generateLibrary(target, def, inputDirectory, outputDirectory, tmpDirectory, logger)
                    if (cacheDirectory != null) {
                        buildCache(target, def, outputDirectory, cacheDirectory, cacheKind, cacheArgs, logger)
                    }

                    built[def] = ProcessingStatus.SUCCESS
                } catch (e: Throwable) {
                    built[def] = ProcessingStatus.FAIL(e)
                    logger.logStackTrace(e)
                }
            }
        }
        executorPool.shutdown()
        executorPool.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS)

        if (built.values.any { it != ProcessingStatus.SUCCESS }) {
            logger.logFailedLibraries(built)
            exitProcess(-1)
        }

    } finally {
        if (!saveTemps) {
            tmpDirectory.deleteRecursively()
        }
        Runtime.getRuntime().removeShutdownHook(deleteTmpHook)
    }
}