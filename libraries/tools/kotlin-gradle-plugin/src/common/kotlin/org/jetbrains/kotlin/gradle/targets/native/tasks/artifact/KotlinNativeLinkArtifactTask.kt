/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.tasks.artifact

import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.*
import org.gradle.process.ExecOperations
import org.jetbrains.kotlin.compilerRunner.KotlinNativeCompilerRunner
import org.jetbrains.kotlin.compilerRunner.KotlinToolRunner
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonToolOptions
import org.jetbrains.kotlin.gradle.internal.ensureParentDirsCreated
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.BitcodeEmbeddingMode
import org.jetbrains.kotlin.gradle.targets.native.tasks.buildKotlinNativeBinaryLinkerArgs
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.util.visibleName
import java.io.File
import javax.inject.Inject

open class KotlinNativeLinkArtifactTask @Inject constructor(
    @get:Input val konanTarget: KonanTarget,
    @get:Input val outputKind: CompilerOutputKind,
    private val objectFactory: ObjectFactory,
    private val execOperations: ExecOperations
) : DefaultTask() {

    @get:Input
    var baseName: String = project.name

    private val defaultDestinationDir: File
        get() {
            val kind = outputKind.visibleName
            val target = konanTarget.visibleName
            val type = if (debuggable) "debug" else "release"
            return project.buildDir.resolve("out/$kind/$target/$type")
        }

    private var customDestinationDir: File? = null

    @get:OutputDirectory
    var destinationDir: File
        get() = customDestinationDir ?: defaultDestinationDir
        set(value) {
            customDestinationDir = value
        }

    @get:Input
    var optimized: Boolean = false

    @get:Input
    var debuggable: Boolean = true

    @get:Input
    var enableEndorsedLibs: Boolean = false

    @get:Input
    var processTests: Boolean = false

    @get:Optional
    @get:Input
    var entryPoint: String? = null

    @get:Input
    var isStaticFramework: Boolean = false

    @get:Input
    var embedBitcode: BitcodeEmbeddingMode = BitcodeEmbeddingMode.DISABLE

    @get:Internal
    var librariesConfiguration: String? = null

    @get:Classpath
    val libraries: FileCollection by lazy {
        librariesConfiguration?.let {
            project.configurations.getByName(it)
        } ?: project.objects.fileCollection()
    }

    @get:Internal
    var exportLibrariesConfiguration: String? = null

    @get:Classpath
    val exportLibraries: FileCollection by lazy {
        exportLibrariesConfiguration?.let {
            project.configurations.getByName(it)
        } ?: project.objects.fileCollection()
    }

    @get:Internal
    var includeLibrariesConfiguration: String? = null

    @get:Classpath
    val includeLibraries: FileCollection by lazy {
        includeLibrariesConfiguration?.let {
            project.configurations.getByName(it)
        } ?: project.objects.fileCollection()
    }

    @get:Input
    var linkerOptions: List<String> = emptyList()

    @get:Input
    var binaryOptions: Map<String, String> = emptyMap()

    @get:Internal
    val kotlinOptions = object : KotlinCommonToolOptions {
        override var allWarningsAsErrors: Boolean = false
        override var suppressWarnings: Boolean = false
        override var verbose: Boolean = false
        override var freeCompilerArgs: List<String> = PropertiesProvider(project).nativeLinkArgs
    }

    fun kotlinOptions(fn: KotlinCommonToolOptions.() -> Unit) {
        kotlinOptions.fn()
    }

    fun kotlinOptions(fn: Action<KotlinCommonToolOptions>) {
        fn.execute(kotlinOptions)
    }

    @get:Input
    val allWarningsAsErrors: Boolean
        get() = kotlinOptions.allWarningsAsErrors

    @get:Input
    val suppressWarnings: Boolean
        get() = kotlinOptions.suppressWarnings

    @get:Input
    val verbose: Boolean
        get() = kotlinOptions.verbose

    @get:Input
    val freeCompilerArgs: List<String>
        get() = kotlinOptions.freeCompilerArgs

    @get:Internal
    val outputFile: File
        get() {
            val outFileName = "${outputKind.prefix(konanTarget)}$baseName${outputKind.suffix(konanTarget)}".replace('-', '_')
            return destinationDir.resolve(outFileName)
        }

    @get:Nested
    internal val runnerSettings = KotlinNativeCompilerRunner.Settings(project)

    @TaskAction
    fun link() {
        val outFile = outputFile
        outFile.ensureParentDirsCreated()

        fun FileCollection.klibs() = files.filter { it.extension == "klib" }

        val localBinaryOptions = PropertiesProvider(project).nativeBinaryOptions + binaryOptions

        val buildArgs = buildKotlinNativeBinaryLinkerArgs(
            outFile = outFile,
            optimized = optimized,
            debuggable = debuggable,
            target = konanTarget,
            outputKind = outputKind,
            libraries = libraries.klibs(),
            friendModules = emptyList(), //FriendModules aren't needed here because it's no test artifact
            enableEndorsedLibs = enableEndorsedLibs,
            kotlinOptions = kotlinOptions,
            compilerPlugins = emptyList(),//CompilerPlugins aren't needed here because it's no compilation but linking
            processTests = processTests,
            entryPoint = entryPoint,
            embedBitcode = embedBitcode,
            linkerOpts = linkerOptions,
            binaryOptions = localBinaryOptions,
            isStaticFramework = isStaticFramework,
            exportLibraries = exportLibraries.klibs(),
            includeLibraries = includeLibraries.klibs(),
            additionalOptions = emptyList()//todo support org.jetbrains.kotlin.gradle.tasks.CacheBuilder and org.jetbrains.kotlin.gradle.tasks.ExternalDependenciesBuilder
        )

        KotlinNativeCompilerRunner(
            settings = runnerSettings,
            executionContext = KotlinToolRunner.GradleExecutionContext.fromTaskContext(objectFactory, execOperations, logger)
        ).run(buildArgs)
    }
}