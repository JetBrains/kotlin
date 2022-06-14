/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20.cinterop

import org.apache.tools.ant.types.PropertySet
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.commonizer.*
import org.jetbrains.kotlin.compilerRunner.GradleCliCommonizer
import org.jetbrains.kotlin.compilerRunner.KotlinNativeCInteropRunner
import org.jetbrains.kotlin.compilerRunner.KotlinNativeCInteropRunner.Companion.run
import org.jetbrains.kotlin.compilerRunner.konanHome
import org.jetbrains.kotlin.gradle.targets.native.internal.additionalCommonizerSettings
import org.jetbrains.kotlin.gradle.targets.native.internal.commonizerLogLevel
import org.jetbrains.kotlin.gradle.targets.native.internal.getNativeDistributionDependencies
import org.jetbrains.kotlin.gradle.targets.native.tasks.createExecutionContext
import org.jetbrains.kotlin.gradle.tasks.*
import org.jetbrains.kotlin.gradle.utils.klibModuleName
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.File

abstract class DefaultTaskWithOutputFile : DefaultTask() {
    @get:OutputFile
    abstract val outputFile: Provider<File>
}

@Suppress("LeakingThis")
abstract class CinteropTask : DefaultTaskWithOutputFile() {

    @get:Input
    abstract val interopName: Property<String>

    @get:Input
    abstract val target: Property<KonanTarget>

    @get:InputFile
    abstract val defFile: RegularFileProperty

    @get:Input
    @get:Optional
    abstract val packageName: Property<String>

    @get:Input
    abstract val moduleName: Property<String>

    @get:IgnoreEmptyDirectories
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val headers: ConfigurableFileCollection

    @get:IgnoreEmptyDirectories
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val allHeadersDirs: ConfigurableFileCollection

    @get:IgnoreEmptyDirectories
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val headerFilterDirs: ConfigurableFileCollection

    @get:IgnoreEmptyDirectories
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val libraries: ConfigurableFileCollection

    @get:Input
    abstract val compilerOpts: SetProperty<String>

    @get:Input
    abstract val linkerOpts: SetProperty<String>

    @get:Input
    abstract val extraOpts: SetProperty<String>

    @get:Internal
    abstract val outputDir: DirectoryProperty

    @get:Input
    abstract val artifactName: Property<String>

    @get:OutputFile
    override val outputFile: Provider<File> = outputDir.map { it.asFile.resolve(artifactName.get() + ".klib") }

    init {
        //default values
        defFile.convention(project.layout.projectDirectory.file(interopName.map { "src/nativeInterop/cinterop/$it.def" }))
        moduleName.convention(artifactName.map { project.klibModuleName(it) })
        artifactName.convention(interopName.map { "${project.name}-cinterop-$it" })
        outputDir.convention(project.layout.buildDirectory.dir(target.map { "classes/kotlin/${it.name}/cinterop" }))
    }

    @TaskAction
    fun run() {
        val args = mutableListOf<String>().apply {
            addArg("-o", outputFile.get().absolutePath)
            addArg("-target", target.get().visibleName)
            addArg("-def", defFile.get().asFile.absolutePath)
            addArgIfNotNull("-pkg", packageName.orNull)
            addFileArgs("-header", headers)
            addArgs("-compiler-option", compilerOpts.get())
            addArgs("-linker-option", linkerOpts.get())
            addArgs("-library", libraries.files.filterKlibsPassedToCompiler().map { it.absolutePath })
            addArgs("-compiler-option", allHeadersDirs.map { "-I${it.absolutePath}" })
            addArgs("-headerFilterAdditionalSearchPrefix", headerFilterDirs.map { it.absolutePath })
            addArg("-Xmodule-name", moduleName.get())
            addAll(extraOpts.get())
        }

        outputDir.get().asFile.mkdirs()
        KotlinNativeCInteropRunner.createExecutionContext(this).run(args)
        logger.info("Cinterop library was generated: " + outputFile.get().absolutePath)
    }
}

@Suppress("LeakingThis")
abstract class CommonizerTask : DefaultTaskWithOutputFile() {

    @get:Input
    abstract val libraryName: Property<String>

    @get:Input
    abstract val libraries: MapProperty<KonanTarget, File>

    @get:Input
    abstract val dependencies: SetProperty<CommonizerDependency>

    private val commonizerTarget: Provider<SharedCommonizerTarget> = libraries.keySet().map { SharedCommonizerTarget(it) }

    @get:Internal
    abstract val outputDir: DirectoryProperty

    @get:OutputDirectory
    override val outputFile: Provider<File> = outputDir.map {
        //based on GradleCliCommonizer inner logic
        val inputLibName = libraries.get().values.firstOrNull()?.nameWithoutExtension.orEmpty()
        it.asFile.resolve(commonizerTarget.get().identityString + "/" + inputLibName)
    }

    init {
        //default values
        outputDir.convention(project.layout.buildDirectory.dir(libraryName.map { "classes/kotlin/commonizer/$it" }))
    }

    @TaskAction
    fun run() {
        GradleCliCommonizer(project).commonizeLibraries(
            konanHome = project.file(project.konanHome),
            inputLibraries = libraries.get().values.toSet(),
            outputTargets = setOf(commonizerTarget.get()),
            dependencyLibraries = dependencies.get() + getNativeDistributionDependencies(),
            outputDirectory = outputDir.get().asFile,
            logLevel = project.commonizerLogLevel,
            additionalSettings = project.additionalCommonizerSettings
        )
        logger.info("Common library was generated: " + outputFile.get().absolutePath)
    }

    private fun getNativeDistributionDependencies(): Set<TargetedCommonizerDependency> {
        val allTargets = libraries.get().keys.map { LeafCommonizerTarget(it) } + commonizerTarget.get()
        return allTargets.flatMap { target ->
            project.getNativeDistributionDependencies(target).map { TargetedCommonizerDependency(target, it) }
        }.toSet()
    }
}