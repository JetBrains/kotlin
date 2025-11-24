/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.wasm.internal

import org.gradle.api.artifacts.transform.*
import org.gradle.api.file.*
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.CompileClasspath
import org.gradle.api.tasks.Internal
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.compilerRunner.*
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.report.ReportingSettings
import org.jetbrains.kotlin.gradle.targets.js.internal.LibraryFilterCachingService
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilerExecutionStrategy
import org.jetbrains.kotlin.library.impl.isKotlinLibrary
import java.io.File
import javax.inject.Inject

abstract class WasmBinaryTransform : TransformAction<WasmBinaryTransform.Parameters> {
    abstract class Parameters : TransformParameters {
        @get:Internal
        abstract val compilerOptions: Property<K2JSCompilerArguments>

        @get:Internal
        abstract val currentJvmJdkToolsJar: Property<File>

        @get:Classpath
        abstract val defaultCompilerClasspath: ConfigurableFileCollection

        @get:Internal
        abstract val languageVersion: Property<String>

        @get:Internal
        abstract val kotlinPluginVersion: Property<String>

        @get:Internal
        abstract val pathProvider: Property<String>

        @get:Internal
        abstract val projectRootFile: Property<File>

        @get:Internal
        abstract val clientIsAliveFlagFile: Property<File>

        @get:Internal
        abstract val sessionFlagFile: Property<File>

        @get:Internal
        abstract val buildDir: Property<File>

        @get:Internal
        abstract val moduleName: Property<String>

        @get:Internal
        internal abstract val libraryFilterCacheService: Property<LibraryFilterCachingService>

        @get:Internal
        internal abstract val enhancedFreeCompilerArgs: ListProperty<String>
    }

    @get:Inject
    abstract val fs: FileSystemOperations

    @get:Inject
    abstract val archiveOperations: ArchiveOperations

    @get:InputArtifact
    abstract val inputArtifact: Provider<FileSystemLocation>

    @get:CompileClasspath
    @get:InputArtifactDependencies
    abstract val dependencies: FileCollection

    override fun transform(outputs: TransformOutputs) {
        val inputFile = inputArtifact.get().asFile
        val outputDir = outputs.dir(inputFile.name.replace(".klib", "-transformed"))

        val isKotlinLibrary = parameters.libraryFilterCacheService.get().getOrCompute(
            LibraryFilterCachingService.LibraryFilterCacheKey(
                inputFile
            ),
            ::isKotlinLibrary
        )

        if (!isKotlinLibrary) {
            fs.copy {
                it.from(archiveOperations.zipTree(inputFile))
                it.into(outputDir)
            }
            return
        }

        val args = parameters.compilerOptions.get()
        args.apply {
            this.outputDir = outputDir.absolutePath
            moduleName = inputFile.nameWithoutExtension
            includes = inputFile.absolutePath
            libraries = dependencies.files.plus(inputFile).joinToString(File.pathSeparator) { it.absolutePath }
        }

        args.freeArgs += parameters.enhancedFreeCompilerArgs.get()

        args.debuggerCustomFormatters = true
        args.main = "call"
        args.irProduceJs = true
        args.sourceMap = true
        args.sourceMapEmbedSources = "never"
        args.target = "es5"
        args.irModuleName = null

        args.freeArgs = args.freeArgs.filterNot { it.startsWith("-Xir-module-name=") }

        println("TRANSFORMING ")
        println(ArgumentUtils.convertArgumentsToStringList(args))

        val workArgs = GradleKotlinCompilerWorkArguments(
            projectFiles = ProjectFilesForCompilation(
                parameters.projectRootFile.get(),
                parameters.clientIsAliveFlagFile.get(),
                parameters.sessionFlagFile.get(),
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
                null,
                KotlinCompilerExecutionStrategy.DAEMON,
                false,
                generateCompilerRefIndex = false,
            ),
            errorsFiles = null,
            kotlinPluginVersion = parameters.kotlinPluginVersion.get(),
            //no need to log warnings in MessageCollector hear it will be logged by compiler
            kotlinLanguageVersion = parameters.languageVersion.orNull?.let { v ->
                KotlinVersion.fromVersion(
                    v
                )
            } ?: KotlinVersion.DEFAULT,
            compilerArgumentsLogLevel = KotlinCompilerArgumentsLogLevel.DEFAULT,
        )

        GradleKotlinCompilerWork(
            workArgs
        ).run()
    }

    companion object {
        const val ARTIFACT_TYPE = "wasm-binary"
    }
}

abstract class NoOpWasmBinaryTransform : TransformAction<TransformParameters.None> {

    override fun transform(p0: TransformOutputs) {
    }
}