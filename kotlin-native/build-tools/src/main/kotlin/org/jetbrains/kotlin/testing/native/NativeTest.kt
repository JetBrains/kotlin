/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.testing.native

import groovy.lang.Closure
import java.io.File
import javax.inject.Inject
import org.gradle.api.*
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.tasks.*
import org.gradle.api.model.ObjectFactory
import org.gradle.process.ExecOperations
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import org.jetbrains.kotlin.ExecClang
import org.jetbrains.kotlin.konan.target.*
import java.io.OutputStream

interface CompileNativeTestParameters : WorkParameters {
    var mainFile: File
    var inputFiles: List<File>
    var llvmLinkOutputFile: File
    var compilerOutputFile: File
    var targetName: String
    var compilerArgs: List<String>
    var linkCommands: List<List<String>>

    var konanHome: File
    var llvmDir: File
    var experimentalDistribution: Boolean
    var isInfoEnabled: Boolean
}

abstract class CompileNativeTestJob : WorkAction<CompileNativeTestParameters> {
    @get:Inject
    abstract val execOperations: ExecOperations

    @get:Inject
    abstract val objects: ObjectFactory

    private fun llvmLink() {
        with(parameters) {
            val tmpOutput = File.createTempFile("runtimeTests", ".bc").apply {
                deleteOnExit()
            }

            // The runtime provides our implementations for some standard functions (see StdCppStubs.cpp).
            // We need to internalize these symbols to avoid clashes with symbols provided by the C++ stdlib.
            // But llvm-link -internalize is kinda broken: it links modules one by one and can't see usages
            // of a symbol in subsequent modules. So it will mangle such symbols causing "unresolved symbol"
            // errors at the link stage. So we have to run llvm-link twice: the first one links all modules
            // except the one containing the entry point to a single *.bc without internalization. The second
            // run internalizes this big module and links it with a module containing the entry point.
            execOperations.exec {
                executable = "$llvmDir/bin/llvm-link"
                args = listOf("-o", tmpOutput.absolutePath) + inputFiles.map { it.absolutePath }
            }

            execOperations.exec {
                executable = "$llvmDir/bin/llvm-link"
                args = listOf(
                        "-o", llvmLinkOutputFile.absolutePath,
                        mainFile.absolutePath,
                        tmpOutput.absolutePath,
                        "-internalize"
                )
            }
        }
    }

    private fun compile() {
        with(parameters) {
            val platformManager = PlatformManager(buildDistribution(konanHome.absolutePath), experimentalDistribution)
            val execClang = ExecClang.create(objects, platformManager, llvmDir)
            val target = platformManager.targetByName(targetName)

            val clangFlags = buildClangFlags(platformManager.platform(target).configurables)

            if (target.family.isAppleFamily) {
                execClang.execToolchainClang(target) {
                    executable = "clang++"
                    this.args = clangFlags + compilerArgs + listOf(llvmLinkOutputFile.absolutePath, "-o", compilerOutputFile.absolutePath)
                }
            } else {
                execClang.execBareClang {
                    executable = "clang++"
                    this.args = clangFlags + compilerArgs + listOf(llvmLinkOutputFile.absolutePath, "-o", compilerOutputFile.absolutePath)
                }
            }
        }
    }

    private fun link() {
        with(parameters) {
            for (command in linkCommands) {
                execOperations.exec {
                    commandLine(command)
                    if (!isInfoEnabled && command[0].endsWith("dsymutil")) {
                        // Suppress dsymutl's warnings.
                        // See: https://bugs.swift.org/browse/SR-11539.
                        val nullOutputStream = object: OutputStream() {
                            override fun write(b: Int) {}
                        }
                        errorOutput = nullOutputStream
                    }
                }
            }
        }
    }

    override fun execute() {
        llvmLink()
        compile()
        link()
    }
}

abstract class CompileNativeTest @Inject constructor(
        baseName: String,
        @Input val target: KonanTarget,
        @InputFile val mainFile: File,
        private val platformManager: PlatformManager,
        private val mimallocEnabled: Boolean,
) : DefaultTask() {

    @SkipWhenEmpty
    @InputFiles
    val inputFiles: ConfigurableFileCollection = project.files()

    @OutputFile
    var llvmLinkOutputFile: File = project.buildDir.resolve("bitcode/test/${target.name}/$baseName.bc")

    @OutputFile
    var compilerOutputFile: File = project.buildDir.resolve("bin/test/${target.name}/$baseName.o")

    private val executableExtension: String = when (target) {
        is KonanTarget.MINGW_X64 -> ".exe"
        is KonanTarget.MINGW_X86 -> ".exe"
        else -> ""
    }

    @OutputFile
    var outputFile: File = project.buildDir.resolve("bin/test/${target.name}/$baseName$executableExtension")

    @Input
    val clangArgs = mutableListOf<String>()

    @Input
    val linkerArgs = mutableListOf<String>()

    @Input @Optional
    var sanitizer: SanitizerKind? = null

    private val sanitizerFlags = when (sanitizer) {
        null -> listOf()
        SanitizerKind.ADDRESS -> listOf("-fsanitize=address")
        SanitizerKind.THREAD -> listOf("-fsanitize=thread")
    }

    @get:Input
    val linkCommands: List<List<String>>
        get() {
            // Getting link commands requires presence of a target toolchain.
            // Thus we cannot get them at the configuration stage because the toolchain may be not downloaded yet.
            val linker = platformManager.platform(target).linker
            return linker.finalLinkCommands(
                    listOf(compilerOutputFile.absolutePath),
                    outputFile.absolutePath,
                    listOf(),
                    linkerArgs,
                    optimize = false,
                    debug = true,
                    kind = LinkerOutputKind.EXECUTABLE,
                    outputDsymBundle = outputFile.absolutePath + ".dSYM",
                    needsProfileLibrary = false,
                    mimallocEnabled = mimallocEnabled,
                    sanitizer = sanitizer
            ).map { it.argsWithExecutable }
        }

    @get:Inject
    abstract val workerExecutor: WorkerExecutor

    @TaskAction
    fun compile() {
        val workQueue = workerExecutor.noIsolation()

        val parameters = { it: CompileNativeTestParameters ->
            it.mainFile = mainFile
            it.inputFiles = inputFiles.map { it }
            it.llvmLinkOutputFile = llvmLinkOutputFile
            it.compilerOutputFile = compilerOutputFile
            it.targetName = target.name
            it.compilerArgs = clangArgs + sanitizerFlags
            it.linkCommands = linkCommands

            it.konanHome = project.project(":kotlin-native").projectDir
            it.llvmDir = project.file(project.findProperty("llvmDir")!!)
            it.isInfoEnabled = logger.isInfoEnabled
        }

        workQueue.submit(CompileNativeTestJob::class.java, parameters)
    }
}

open class RunNativeTest @Inject constructor(
    @Input val testName: String,
    @InputFile val inputFile: File,
) : DefaultTask() {

    @Internal
    var workingDir: File = project.buildDir.resolve("testReports/$testName")

    @OutputFile
    var outputFile: File = workingDir.resolve("report.xml")

    @OutputFile
    var outputFileWithPrefixes: File = workingDir.resolve("report-with-prefixes.xml")

    @Input @Optional
    var filter: String? = project.findProperty("gtest_filter") as? String

    @Input @Optional
    var sanitizer: SanitizerKind? = null

    @InputFile
    var tsanSuppressionsFile = project.file("tsan_suppressions.txt")

    @TaskAction
    fun run() {
        workingDir.mkdirs()

        // Do not run this in workers, because we don't want this task to run in parallel.
        project.exec {
            executable = inputFile.absolutePath

            if (filter != null) {
                args("--gtest_filter=${filter}")
            }
            args("--gtest_output=xml:${outputFile.absolutePath}")
            when (sanitizer) {
                SanitizerKind.THREAD -> {
                    environment("TSAN_OPTIONS", "suppressions=${tsanSuppressionsFile.absolutePath}")
                }
                else -> {} // no action required
            }
        }

        // TODO: Better to use proper XML parsing.
        var contents = outputFile.readText()
        contents = contents.replace("<testsuite name=\"", "<testsuite name=\"${testName}.")
        contents = contents.replace("classname=\"", "classname=\"${testName}.")
        outputFileWithPrefixes.writeText(contents)
    }
}

/**
 * Returns a list of Clang -cc1 arguments (including -cc1 itself) that are used for bitcode compilation in Kotlin/Native.
 *
 * See also: [org.jetbrains.kotlin.backend.konan.BitcodeCompiler]
 */
private fun buildClangFlags(configurables: Configurables): List<String> = mutableListOf<String>().apply {
    require(configurables is ClangFlags)
    addAll(configurables.clangFlags)
    addAll(configurables.clangNooptFlags)
    val targetTriple = if (configurables is AppleConfigurables) {
        configurables.targetTriple.withOSVersion(configurables.osVersionMin)
    } else {
        configurables.targetTriple
    }
    addAll(listOf("-triple", targetTriple.toString()))
}.toList()