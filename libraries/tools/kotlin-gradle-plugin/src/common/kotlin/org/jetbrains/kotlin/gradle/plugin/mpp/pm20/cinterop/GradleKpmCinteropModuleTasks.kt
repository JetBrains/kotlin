/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20.cinterop

import org.gradle.api.DefaultTask
import org.gradle.api.file.*
import org.gradle.api.provider.*
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
import org.jetbrains.kotlin.gradle.utils.listProperty
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.File

@Suppress("LeakingThis")
abstract class CinteropTask : DefaultTask() {

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
    val outputFile: Provider<File> = outputDir.map { it.asFile.resolve(artifactName.get() + ".klib") }

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
abstract class ModuleCommonizerTask : DefaultTask() {

    @get:Input
    abstract val libraryName: Property<String>

    @get:Input
    abstract val libraries: MapProperty<KonanTarget, File>

    //'libraries' input checks only file paths but content makes sense too
    @get:IgnoreEmptyDirectories
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val inputLibraryFiles: Collection<File>
        get() = libraries.get().values

    @get:Input
    abstract val dependencies: SetProperty<CommonizerDependency>

    @get:Input
    abstract val targets: SetProperty<SharedCommonizerTarget>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    init {
        //default values
        outputDir.convention(project.layout.buildDirectory.dir(libraryName.map { "classes/kotlin/commonizer/$it" }))
    }

    @TaskAction
    fun run() {
        GradleCliCommonizer(project).commonizeLibraries(
            konanHome = project.file(project.konanHome),
            inputLibraries = libraries.get().values.toSet(),
            outputTargets = targets.get(),
            dependencyLibraries = dependencies.get() + getNativeDistributionDependencies(),
            outputDirectory = outputDir.get().asFile,
            logLevel = project.commonizerLogLevel,
            additionalSettings = project.additionalCommonizerSettings
        )
    }

    private fun getNativeDistributionDependencies(): Set<TargetedCommonizerDependency> {
        val allTargets = libraries.get().keys.map { LeafCommonizerTarget(it) } + targets.get()
        return allTargets.flatMap { target ->
            project.getNativeDistributionDependencies(target).map { TargetedCommonizerDependency(target, it) }
        }.toSet()
    }
}

internal abstract class DumbCommonizerTask : DefaultTask() {

    @get:OutputDirectory
    abstract val outputFile: Property<File>

    @TaskAction
    fun run() {
        //NOTE! Commonization should be invoked via ModuleCommonizerTask due performance optimization
        logger.info("Common library was generated: " + outputFile.get().absolutePath)
    }
}
