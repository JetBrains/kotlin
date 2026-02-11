/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.wasm.internal

import org.gradle.api.artifacts.transform.*
import org.gradle.api.file.*
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.process.ExecOperations
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.copyOf
import org.jetbrains.kotlin.compilerRunner.*
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.report.ReportingSettings
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBinaryMode
import org.jetbrains.kotlin.gradle.targets.js.internal.LibraryFilterCachingService
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilerExecutionStrategy
import org.jetbrains.kotlin.gradle.tasks.normalizeJvmArgs
import org.jetbrains.kotlin.gradle.utils.getFile
import org.jetbrains.kotlin.gradle.utils.listFilesOrEmpty
import org.jetbrains.kotlin.library.loader.KlibLoader
import org.jetbrains.kotlin.platform.wasm.BinaryenConfig
import java.io.File
import java.nio.file.Files
import javax.inject.Inject


/**
 * Gradle Artifact Transform that converts Kotlin/Wasm KLib files into Wasm binaries.
 *
 * This transform is essential for preparing external KLib dependencies (like library files)
 * so they can be run in a WebAssembly environment. It uses the Kotlin/JS (IR) compiler
 * to generate the Wasm artifact from the input KLib.
 *
 * In [KotlinJsBinaryMode.PRODUCTION], it also runs Binaryen optimizations on the generated Wasm file.
 */
@CacheableTransform
internal abstract class WasmBinaryTransform : TransformAction<WasmBinaryTransform.Parameters> {
    /**
     * Parameters for the [WasmBinaryTransform].
     */
    abstract class Parameters : TransformParameters {
        @get:Internal
        abstract val compilerOptions: Property<K2JSCompilerArguments>

        @get:Internal
        abstract val currentJvmJdkToolsJar: Property<File>

        @get:Classpath
        abstract val defaultCompilerClasspath: ConfigurableFileCollection

        @get:Input
        abstract val kotlinPluginVersion: Property<String>

        @get:Internal
        abstract val pathProvider: Property<String>

        @get:Internal
        abstract val projectRootFile: Property<File>

        @get:Internal
        internal abstract val projectName: Property<String>

        @get:Internal
        internal abstract val projectSessionsDir: DirectoryProperty

        @get:Internal
        abstract val buildDir: Property<File>

        @get:Internal
        internal abstract val libraryFilterCacheService: Property<LibraryFilterCachingService>

        @get:Input
        internal abstract val enhancedFreeCompilerArgs: ListProperty<String>

        @get:Classpath
        internal abstract val classpath: ConfigurableFileCollection

        @get:Input
        @get:Optional
        internal abstract val invalidate: Property<String>

        @get:Internal
        internal abstract val kotlinDaemonJvmArguments: ListProperty<String>

        @get:Internal
        internal abstract val compilerExecutionStrategy: Property<KotlinCompilerExecutionStrategy>

        @get:Internal
        internal abstract val useDaemonFallbackStrategy: Property<Boolean>

        @get:Internal
        internal abstract val binaryenExec: Property<String>

        @get:Input
        internal abstract val mode: Property<KotlinJsBinaryMode>
    }

    @get:Inject
    abstract val fs: FileSystemOperations

    @get:Inject
    abstract val archiveOperations: ArchiveOperations

    @get:Inject
    abstract val execOps: ExecOperations

    @get:InputArtifact
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val inputArtifact: Provider<FileSystemLocation>

    override fun transform(outputs: TransformOutputs) {
        val inputFile = inputArtifact.get().asFile

        val mode: KotlinJsBinaryMode = parameters.mode.get()
        val compilerOutputDir = when (mode) {
            KotlinJsBinaryMode.PRODUCTION -> Files.createTempDirectory("wasm-transform-").toFile()
            KotlinJsBinaryMode.DEVELOPMENT -> outputs.dir(inputFile.name.replace(".klib", "-transformed"))
        }

        val isKotlinLibrary = isKotlinLibrary(inputFile)

        if (!isKotlinLibrary) {
            fs.copy {
                it.from(archiveOperations.zipTree(inputFile))
                it.into(compilerOutputDir)
            }
            return
        }

        val workArgs = prepareWasmCompilationArgs(compilerOutputDir, inputFile)

        GradleKotlinCompilerWork(
            workArgs
        ).run()

        if (mode == KotlinJsBinaryMode.DEVELOPMENT) return

        val binaryenOutputDirectory = outputs.dir(inputFile.name.replace(".klib", "-transformed"))

        val inputFileBinaryen = compilerOutputDir.listFilesOrEmpty().first { it.extension == "wasm" }
        execOps.exec {
            it.executable = parameters.binaryenExec.get()
            it.workingDir = binaryenOutputDirectory
            it.args = binaryenArgs(inputFileBinaryen, binaryenOutputDirectory)
        }

        fs.copy {
            it.from(compilerOutputDir)
            it.into(binaryenOutputDirectory)
            it.include("*.mjs", "*.js", "*.js.map")
        }

    }

    private fun binaryenArgs(
        inputFileBinaryen: File,
        binaryenOutputDirectory: File,
    ): MutableList<String> {
        val newArgs = mutableListOf<String>()
        newArgs.addAll(BinaryenConfig.binaryenMultimoduleArgs)
        newArgs.add(inputFileBinaryen.absolutePath)
        newArgs.add("-o")
        newArgs.add(binaryenOutputDirectory.resolve(inputFileBinaryen.name).absolutePath)
        return newArgs
    }

    private fun prepareWasmCompilationArgs(
        compilerOutputDir: File,
        inputFile: File,
    ): GradleKotlinCompilerWorkArguments {
        val args = parameters.compilerOptions.get().copyOf()
        args.apply {
            this.outputDir = compilerOutputDir.absolutePath
            moduleName = inputFile.nameWithoutExtension
            includes = inputFile.absolutePath
            libraries = parameters.classpath.files
                .filter { isKotlinLibrary(it) }
                .joinToString(File.pathSeparator) { it.absolutePath }
        }

        args.freeArgs += parameters.enhancedFreeCompilerArgs.get()

        val normalizedKotlinDaemonJvmArguments = parameters.kotlinDaemonJvmArguments.map {
            normalizeJvmArgs(it)
        }

        val workArgs = GradleKotlinCompilerWorkArguments(
            projectFiles = ProjectFilesForCompilation(
                parameters.projectRootFile.get(),
                GradleCompilerRunner.getOrCreateClientFlagFile(LOGGER, parameters.projectName.get()),
                GradleCompilerRunner.getOrCreateSessionFlagFile(LOGGER, parameters.projectSessionsDir.getFile()),
                parameters.buildDir.get(),
            ),
            compilerFullClasspath = (parameters.defaultCompilerClasspath.files + parameters.currentJvmJdkToolsJar.orNull).filterNotNull(),
            compilerClassName = KotlinCompilerClass.JS,
            compilerArgs = ArgumentUtils.convertArgumentsToStringList(args).toTypedArray(),
            isVerbose = false,
            incrementalCompilationEnvironment = null,
            incrementalModuleInfo = null,
            outputFiles = emptyList(),
            taskPath = parameters.pathProvider.get(),
            reportingSettings = ReportingSettings(),
            kotlinScriptExtensions = emptyArray(),
            allWarningsAsErrors = false,
            compilerExecutionSettings = CompilerExecutionSettings(
                normalizedKotlinDaemonJvmArguments.orNull,
                parameters.compilerExecutionStrategy.get(),
                parameters.useDaemonFallbackStrategy.get(),
                generateCompilerRefIndex = false,
            ),
            errorsFiles = null,
            kotlinPluginVersion = parameters.kotlinPluginVersion.get(),
            // no need to log warnings in MessageCollector here, it will be logged by compiler
            kotlinLanguageVersion = args.languageVersion?.let { v ->
                KotlinVersion.fromVersion(
                    v
                )
            } ?: KotlinVersion.DEFAULT,
            compilerArgumentsLogLevel = KotlinCompilerArgumentsLogLevel.DEFAULT,
        )
        return workArgs
    }

    private fun isKotlinLibrary(file: File): Boolean {
        return parameters.libraryFilterCacheService.get().getOrCompute(
            LibraryFilterCachingService.LibraryFilterCacheKey(
                file
            )
        ) {
            KlibLoader { libraryPaths(it.absolutePath) }.load().librariesStdlibFirst.isNotEmpty()
        }
    }

    private companion object {
        private val LOGGER: Logger = Logging.getLogger(WasmBinaryTransform::class.java)
    }
}

/**
 * A no-op version of [WasmBinaryTransform] that can be used when transformation is not needed
 * or when the input is already in the expected format.
 */
@CacheableTransform
internal abstract class NoOpWasmBinaryTransform : TransformAction<TransformParameters.None> {

    override fun transform(p0: TransformOutputs) {
    }
}