/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.tasks.artifact

import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.process.ExecOperations
import org.jetbrains.kotlin.compilerRunner.KotlinNativeCompilerRunner
import org.jetbrains.kotlin.compilerRunner.KotlinToolRunner
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.internal.ensureParentDirsCreated
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.BitcodeEmbeddingMode
import org.jetbrains.kotlin.gradle.targets.native.tasks.buildKotlinNativeBinaryLinkerArgs
import org.jetbrains.kotlin.gradle.tasks.KotlinToolTask
import org.jetbrains.kotlin.gradle.utils.newInstance
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.util.visibleName
import java.io.File
import javax.inject.Inject

@Suppress("LeakingThis")
abstract class KotlinNativeLinkArtifactTask @Inject constructor(
    @get:Input val konanTarget: KonanTarget,
    @get:Input val outputKind: CompilerOutputKind,
    private val objectFactory: ObjectFactory,
    private val execOperations: ExecOperations,
    private val projectLayout: ProjectLayout
) : DefaultTask(),
    KotlinToolTask<KotlinCommonCompilerToolOptions> {

    @get:Input
    abstract val baseName: Property<String>

    @get:OutputDirectory
    abstract val destinationDir: DirectoryProperty

    @get:Input
    abstract val optimized: Property<Boolean>

    @get:Input
    abstract val debuggable: Property<Boolean>

    @Deprecated("Please declare explicit dependency on kotlinx-cli. This option is scheduled to be removed in 1.9.0")
    @get:Input
    abstract val enableEndorsedLibs: Property<Boolean>

    @get:Input
    abstract val processTests: Property<Boolean>

    @get:Optional
    @get:Input
    abstract val entryPoint: Property<String>

    @get:Input
    abstract val staticFramework: Property<Boolean>

    @get:Input
    abstract val embedBitcode: Property<BitcodeEmbeddingMode>

    @get:Classpath
    abstract val libraries: ConfigurableFileCollection

    @get:Classpath
    abstract val exportLibraries: ConfigurableFileCollection

    @get:Classpath
    abstract val includeLibraries: ConfigurableFileCollection

    @get:Input
    abstract val linkerOptions: ListProperty<String>

    @get:Input
    abstract val binaryOptions: MapProperty<String, String>

    private val nativeBinaryOptions = PropertiesProvider(project).nativeBinaryOptions

    @get:Input
    internal val allBinaryOptions: Provider<Map<String, String>> = binaryOptions.map { it + nativeBinaryOptions }

    override val toolOptions: KotlinCommonCompilerToolOptions = objectFactory
        .newInstance<KotlinCommonCompilerToolOptionsDefault>()
        .apply {
            freeCompilerArgs.addAll(PropertiesProvider(project).nativeLinkArgs)
        }

    @Suppress("DEPRECATION")
    @Deprecated(
        message = "Replaced with toolOptions",
        replaceWith = ReplaceWith("toolOptions")
    )
    @get:Internal
    val kotlinOptions = object : KotlinCommonToolOptions {
        override val options: KotlinCommonCompilerToolOptions
            get() = toolOptions
    }

    @Suppress("DEPRECATION")
    @Deprecated(
        message = "Replaced with toolOptions()",
        replaceWith = ReplaceWith("toolOptions(fn)")
    )
    fun kotlinOptions(fn: KotlinCommonToolOptions.() -> Unit) {
        kotlinOptions.fn()
    }

    @Suppress("DEPRECATION")
    @Deprecated(
        message = "Replaced with toolOptions()",
        replaceWith = ReplaceWith("toolOptions(fn)")
    )
    fun kotlinOptions(fn: Action<KotlinCommonToolOptions>) {
        fn.execute(kotlinOptions)
    }

    @Deprecated(
        message = "Replaced with toolOptions.allWarningsAsErrors",
        replaceWith = ReplaceWith("toolOptions.allWarningsAsErrors")
    )
    @get:Internal
    val allWarningsAsErrors: Boolean
        get() = toolOptions.allWarningsAsErrors.get()

    @Deprecated(
        message = "Replaced with toolOptions.suppressWarnings",
        replaceWith = ReplaceWith("toolOptions.suppressWarnings")
    )
    @get:Internal
    val suppressWarnings: Boolean
        get() = toolOptions.suppressWarnings.get()

    @Deprecated(
        message = "Replaced with toolOptions.verbose",
        replaceWith = ReplaceWith("toolOptions.verbose")
    )
    @get:Internal
    val verbose: Boolean
        get() = toolOptions.verbose.get()

    @Deprecated(
        message = "Replaced with toolOptions.freeCompilerArgs",
        replaceWith = ReplaceWith("toolOptions.freeCompilerArgs")
    )
    @get:Internal
    val freeCompilerArgs: List<String>
        get() = toolOptions.freeCompilerArgs.get()

    @get:Internal
    val outputFile: Provider<File> = project.provider {
        val outFileName = "${outputKind.prefix(konanTarget)}${baseName.get()}${outputKind.suffix(konanTarget)}".replace('-', '_')
        destinationDir.asFile.get().resolve(outFileName)
    }

    private val runnerSettings = KotlinNativeCompilerRunner.Settings.fromProject(project)

    init {
        baseName.convention(project.name)
        debuggable.convention(true)
        optimized.convention(false)
        enableEndorsedLibs.convention(false)
        processTests.convention(false)
        staticFramework.convention(false)
        embedBitcode.convention(BitcodeEmbeddingMode.DISABLE)
        destinationDir.convention(debuggable.flatMap {
            val kind = outputKind.visibleName
            val target = konanTarget.visibleName
            val type = if (it) "debug" else "release"
            projectLayout.buildDirectory.dir("out/$kind/$target/$type")
        })
    }

    @TaskAction
    fun link() {
        val outFile = outputFile.get()
        outFile.ensureParentDirsCreated()

        fun FileCollection.klibs() = files.filter { it.extension == "klib" }

        val buildArgs = buildKotlinNativeBinaryLinkerArgs(
            outFile = outFile,
            optimized = optimized.get(),
            debuggable = debuggable.get(),
            target = konanTarget,
            outputKind = outputKind,
            libraries = libraries.klibs(),
            friendModules = emptyList(), //FriendModules aren't needed here because it's no test artifact
            enableEndorsedLibs = enableEndorsedLibs.get(),  // TODO: remove before 1.9.0, see KT-54098
            toolOptions = toolOptions,
            compilerPlugins = emptyList(),//CompilerPlugins aren't needed here because it's no compilation but linking
            processTests = processTests.get(),
            entryPoint = entryPoint.getOrNull(),
            embedBitcode = embedBitcode.get(),
            linkerOpts = linkerOptions.get(),
            binaryOptions = allBinaryOptions.get(),
            isStaticFramework = staticFramework.get(),
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