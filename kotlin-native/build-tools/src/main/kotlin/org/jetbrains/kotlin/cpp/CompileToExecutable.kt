/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cpp

import org.gradle.api.*
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logging
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.getByType
import org.gradle.process.ExecOperations
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import org.jetbrains.kotlin.ExecClang
import org.jetbrains.kotlin.execLlvmUtility
import org.jetbrains.kotlin.konan.target.*
import java.io.OutputStream
import javax.inject.Inject

private abstract class CompileToExecutableJob : WorkAction<CompileToExecutableJob.Parameters> {
    interface Parameters : WorkParameters {
        val mainFile: RegularFileProperty
        val inputFiles: ConfigurableFileCollection
        val llvmLinkFirstStageOutputFile: RegularFileProperty
        val llvmLinkOutputFile: RegularFileProperty
        val compilerOutputFile: RegularFileProperty
        val outputFile: RegularFileProperty
        // TODO: Figure out a way to pass KonanTarget, but it is used as a key into PlatformManager,
        //       so object identity matters, and platform managers are different between project and worker sides.
        val targetName: Property<String>
        val clangFlags: ListProperty<String>
        val linkCommands: ListProperty<List<String>>
        val platformManager: Property<PlatformManager>
    }

    @get:Inject
    abstract val execOperations: ExecOperations

    @get:Inject
    abstract val objects: ObjectFactory

    private val target: KonanTarget by lazy {
        parameters.platformManager.get().targetByName(parameters.targetName.get())
    }

    private fun llvmLink() {
        with(parameters) {
            llvmLinkFirstStageOutputFile.asFile.get().parentFile.mkdirs()

            // The runtime provides our implementations for some standard functions (see StdCppStubs.cpp).
            // We need to internalize these symbols to avoid clashes with symbols provided by the C++ stdlib.
            // But llvm-link -internalize is kinda broken: it links modules one by one and can't see usages
            // of a symbol in subsequent modules. So it will mangle such symbols causing "unresolved symbol"
            // errors at the link stage. So we have to run llvm-link twice: the first one links all modules
            // except the one containing the entry point to a single *.bc without internalization. The second
            // run internalizes this big module and links it with a module containing the entry point.
            execOperations.execLlvmUtility(platformManager.get(), "llvm-link") {
                args = listOf("-o", llvmLinkFirstStageOutputFile.asFile.get().absolutePath) + inputFiles.map { it.absolutePath }
            }

            llvmLinkOutputFile.asFile.get().parentFile.mkdirs()

            execOperations.execLlvmUtility(platformManager.get(), "llvm-link") {
                args = listOf("-o", llvmLinkOutputFile.asFile.get().absolutePath, mainFile.asFile.get().absolutePath, llvmLinkFirstStageOutputFile.asFile.get().absolutePath, "-internalize")
            }
        }
    }

    private fun compile() {
        with(parameters) {
            val execClang = ExecClang.create(objects, platformManager.get())

            val args = clangFlags.get() + listOf(llvmLinkOutputFile.asFile.get().absolutePath, "-o", compilerOutputFile.asFile.get().absolutePath)

            compilerOutputFile.asFile.get().parentFile.mkdirs()

            if (target.family.isAppleFamily) {
                execClang.execToolchainClang(target) {
                    executable = "clang++"
                    this.args = args
                }
            } else {
                execClang.execBareClang {
                    executable = "clang++"
                    this.args = args
                }
            }
        }
    }

    private fun link() {
        with(parameters) {
            outputFile.asFile.get().parentFile.mkdirs()

            val logging = Logging.getLogger(CompileToExecutableJob::class.java)
            for (command in linkCommands.get()) {
                execOperations.exec {
                    commandLine(command)
                    if (!logging.isInfoEnabled && command[0].endsWith("dsymutil")) {
                        // Suppress dsymutl's warnings.
                        // See: https://bugs.swift.org/browse/SR-11539.
                        val nullOutputStream = object : OutputStream() {
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

/**
 * Compile bitcode files to binary executable.
 *
 * Takes bitcode files [inputFiles] and bitcode file [mainFile] with `main()` entrypoint and produces executable [outputFile].
 *
 * This works in 4 stages:
 * 1. `llvm-link` all [inputFiles] into [llvmLinkFirstStageOutputFile].
 * 2. `llvm-link` [llvmLinkFirstStageOutputFile] and [mainFile] with `-internalize` into [llvmLinkOutputFile].
 * 3. `clang++` [llvmLinkOutputFile] into object file [compilerOutputFile].
 * 4. Link [compilerOutputFile] into executable [outputFile].
 *
 * @see CompileToBitcodePlugin
 */
abstract class CompileToExecutable : DefaultTask() {

    private val platformManager = project.extensions.getByType<PlatformManager>()

    /**
     * Target for which to compile.
     */
    @get:Input
    abstract val target: Property<KonanTarget>

    /**
     * Sanitizer for which to compile.
     */
    @get:Input
    @get:Optional
    abstract val sanitizer: Property<SanitizerKind>

    // TODO: Should be replaced by a list of libraries to be linked with.
    /**
     * Controls whether linker should add library dependencies.
     */
    @get:Input
    abstract val mimallocEnabled: Property<Boolean>

    /**
     * Bitcode file with the `main()` entrypoint.
     */
    @get:InputFile
    abstract val mainFile: RegularFileProperty

    /**
     * Bitcode files.
     *
     * Bitcode file with main should be put into [mainFile] instead.
     */
    @get:SkipWhenEmpty
    @get:InputFiles
    abstract val inputFiles: ConfigurableFileCollection

    /**
     * Internal file with first stage llvm-link result.
     */
    @get:Internal
    abstract val llvmLinkFirstStageOutputFile: RegularFileProperty

    /**
     * Internal file with final stage llvm-link result.
     */
    @get:Internal
    abstract val llvmLinkOutputFile: RegularFileProperty

    /**
     * Internal file with compiler result.
     */
    @get:Internal
    abstract val compilerOutputFile: RegularFileProperty

    /**
     * Final executable.
     */
    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    /**
     * Extra args to the compiler.
     */
    @get:Input
    abstract val compilerArgs: ListProperty<String>

    /**
     * Extra args to the linker.
     */
    @get:Input
    abstract val linkerArgs: ListProperty<String>

    @get:Input
    protected val linkCommands: Provider<List<List<String>>> = project.provider {
        // Getting link commands requires presence of a target toolchain.
        // Thus we cannot get them at the configuration stage because the toolchain may be not downloaded yet.
        platformManager.platform(target.get()).linker.finalLinkCommands(
                listOf(compilerOutputFile.asFile.get().absolutePath),
                outputFile.asFile.get().absolutePath,
                listOf(),
                linkerArgs.get(),
                optimize = false,
                debug = true,
                kind = LinkerOutputKind.EXECUTABLE,
                outputDsymBundle = outputFile.asFile.get().absolutePath + ".dSYM",
                mimallocEnabled = mimallocEnabled.get(),
                sanitizer = sanitizer.orNull
        ).map { it.argsWithExecutable }
    }

    @get:Inject
    protected abstract val workerExecutor: WorkerExecutor

    @TaskAction
    fun compile() {
        val workQueue = workerExecutor.noIsolation()

        val defaultClangFlags = buildClangFlags(platformManager.platform(target.get()).configurables)
        val sanitizerFlags = when (sanitizer.orNull) {
            null -> listOf()
            SanitizerKind.ADDRESS -> listOf("-fsanitize=address")
            SanitizerKind.THREAD -> listOf("-fsanitize=thread")
        }

        workQueue.submit(CompileToExecutableJob::class.java) {
            mainFile.set(this@CompileToExecutable.mainFile)
            inputFiles.from(this@CompileToExecutable.inputFiles)
            llvmLinkFirstStageOutputFile.set(this@CompileToExecutable.llvmLinkFirstStageOutputFile)
            llvmLinkOutputFile.set(this@CompileToExecutable.llvmLinkOutputFile)
            compilerOutputFile.set(this@CompileToExecutable.compilerOutputFile)
            outputFile.set(this@CompileToExecutable.outputFile)
            targetName.set(this@CompileToExecutable.target.get().name)
            clangFlags.addAll(defaultClangFlags + compilerArgs.get() + sanitizerFlags)
            linkCommands.set(this@CompileToExecutable.linkCommands)
            platformManager.set(this@CompileToExecutable.platformManager)
        }
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
