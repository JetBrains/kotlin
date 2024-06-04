/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.KotlinNativeBinaryContainer
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.AppleTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.appleTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.configuration
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal.maybeCreateSwiftExportClasspathResolvableConfiguration
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.tasks.BuildSPMSwiftExportPackage
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.tasks.GenerateSPMPackageFromSwiftExport
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.tasks.MergeStaticLibrariesTask
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.tasks.SwiftExportTask
import org.jetbrains.kotlin.gradle.tasks.locateOrRegisterTask
import org.jetbrains.kotlin.gradle.utils.*
import org.jetbrains.kotlin.gradle.utils.getOrCreate
import org.jetbrains.kotlin.gradle.utils.konanDistribution
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.konan.target.Distribution

internal enum class SwiftExportPipelineType {
    EMBED, FRAMEWORK;
}

internal fun Project.setupCommonSwiftExportPipeline(
    swiftApiModuleName: Provider<String>,
    taskNamePrefix: String,
    target: KotlinNativeTarget,
    buildType: NativeBuildType,
    pipelineType: SwiftExportPipelineType,
    configuration: (
        buildConfiguration: String,
        packageBuild: TaskProvider<BuildSPMSwiftExportPackage>,
        packageGenerationTask: TaskProvider<GenerateSPMPackageFromSwiftExport>,
        mergeLibrariesTask: TaskProvider<MergeStaticLibrariesTask>,
    ) -> TaskProvider<out Task>,
): TaskProvider<out Task> {
    val mainCompilation = target.compilations.getByName("main")
    val buildConfiguration = buildType.configuration

    val swiftExportTask = registerSwiftExportRun(
        taskNamePrefix = taskNamePrefix,
        target = target,
        configuration = buildConfiguration,
        swiftApiModuleName = swiftApiModuleName,
        mainCompilation = mainCompilation
    )
    val staticLibrary = registerSwiftExportCompilationAndGetBinary(
        buildType = buildType,
        compilations = target.compilations,
        binaries = target.binaries,
        mainCompilation = mainCompilation,
        swiftExportTask = swiftExportTask,
    )

    val swiftApiLibraryName = swiftApiModuleName.map { it + "Library" }

    val packageGenerationTask = registerPackageGeneration(
        taskNamePrefix = taskNamePrefix,
        target = target,
        configuration = buildConfiguration,
        swiftApiModuleName = swiftApiModuleName,
        swiftApiLibraryName = swiftApiLibraryName,
        swiftExportTask = swiftExportTask,
    )
    val packageBuild = registerSPMPackageBuild(
        taskNamePrefix = taskNamePrefix,
        target = target,
        configuration = buildConfiguration,
        swiftApiModuleName = swiftApiModuleName,
        swiftApiLibraryName = swiftApiLibraryName,
        packageGenerationTask = packageGenerationTask,
    )
    val mergeLibrariesTask = registerMergeLibraryTask(
        appleTarget = target.konanTarget.appleTarget,
        configuration = buildConfiguration,
        pipelineType = pipelineType,
        staticLibrary = staticLibrary,
        swiftApiModuleName = swiftApiModuleName,
        packageBuildTask = packageBuild
    )

    return configuration(buildConfiguration, packageBuild, packageGenerationTask, mergeLibrariesTask)
}

private fun Project.registerSwiftExportRun(
    taskNamePrefix: String,
    target: KotlinNativeTarget,
    configuration: String,
    swiftApiModuleName: Provider<String>,
    mainCompilation: KotlinNativeCompilation,
): TaskProvider<SwiftExportTask> {
    val swiftExportTaskName = lowerCamelCaseName(
        taskNamePrefix,
        "swiftExport"
    )

    val outputs = layout.buildDirectory.dir("SwiftExport/${target.name}/$configuration")
    val compileTask = mainCompilation.compileTaskProvider

    return locateOrRegisterTask<SwiftExportTask>(swiftExportTaskName) { task ->
        task.description = "Run $taskNamePrefix Swift Export process"

        val files = outputs.map { it.dir("files") }
        val serializedModules = outputs.map { it.dir("modules") }

        // Input
        task.swiftExportClasspath.from(maybeCreateSwiftExportClasspathResolvableConfiguration())
        task.parameters.swiftApiModuleName.convention(swiftApiModuleName)
        task.parameters.bridgeModuleName.convention("SharedBridge")
        task.parameters.konanDistribution.convention(Distribution(konanDistribution.root.absolutePath))
        task.parameters.kotlinLibraryFile.set(
            layout.file(compileTask.map { it.outputFile.get() })
        )

        // Output
        task.parameters.outputPath.set(files)
        task.parameters.swiftModulesFile.set(
            serializedModules.map { it.file("${swiftApiModuleName.get()}.json") }
        )
    }
}

private fun registerSwiftExportCompilationAndGetBinary(
    buildType: NativeBuildType,
    compilations: NamedDomainObjectContainer<KotlinNativeCompilation>,
    binaries: KotlinNativeBinaryContainer,
    mainCompilation: KotlinNativeCompilation,
    swiftExportTask: TaskProvider<SwiftExportTask>,
): AbstractNativeLibrary {
    val swiftExportCompilationName = "swiftExportMain"
    val swiftExportBinary = "SwiftExportBinary"

    compilations.getOrCreate(
        swiftExportCompilationName,
        invokeWhenCreated = { swiftExportCompilation ->
            swiftExportCompilation.associateWith(mainCompilation)
            swiftExportCompilation.defaultSourceSet.kotlin.srcDir(swiftExportTask.map {
                it.parameters.outputPath.getFile()
            })

            swiftExportCompilation.compileTaskProvider.configure {
                it.compilerOptions.optIn.add("kotlin.experimental.ExperimentalNativeApi")
                it.compilerOptions.optIn.add("kotlinx.cinterop.ExperimentalForeignApi")
                it.compilerOptions.optIn.add("kotlin.native.internal.InternalForKotlinNative")
            }

            binaries.staticLib(swiftExportBinary) { staticLib ->
                staticLib.compilation = swiftExportCompilation
                staticLib.binaryOption("swiftExport", "true")
                staticLib.binaryOption("cInterfaceMode", "none")
            }
        }
    )

    return binaries.getStaticLib(
        swiftExportBinary,
        buildType
    )
}

private fun Project.registerPackageGeneration(
    taskNamePrefix: String,
    target: KotlinNativeTarget,
    configuration: String,
    swiftApiModuleName: Provider<String>,
    swiftApiLibraryName: Provider<String>,
    swiftExportTask: TaskProvider<SwiftExportTask>,
): TaskProvider<GenerateSPMPackageFromSwiftExport> {
    val spmPackageGenTaskName = lowerCamelCaseName(
        taskNamePrefix,
        "generateSPMPackage"
    )

    val packageGenerationTask = locateOrRegisterTask<GenerateSPMPackageFromSwiftExport>(spmPackageGenTaskName) { task ->
        task.group = BasePlugin.BUILD_GROUP
        task.description = "Generates $taskNamePrefix SPM Package"

        // Input
        task.kotlinRuntime.set(
            file(Distribution(konanDistribution.root.canonicalPath).kotlinRuntimeForSwiftHome)
        )

        task.swiftModulesFile.set(swiftExportTask.map { it.parameters.swiftModulesFile.get() })
        task.swiftLibraryName.set(swiftApiLibraryName)
        task.swiftApiModuleName.set(swiftApiModuleName)

        // Output
        task.packagePath.set(layout.buildDirectory.dir("SPMPackage/${target.name}/$configuration"))
    }

    return packageGenerationTask
}

private fun Project.registerSPMPackageBuild(
    taskNamePrefix: String,
    target: KotlinNativeTarget,
    configuration: String,
    swiftApiModuleName: Provider<String>,
    swiftApiLibraryName: Provider<String>,
    packageGenerationTask: TaskProvider<GenerateSPMPackageFromSwiftExport>,
): TaskProvider<BuildSPMSwiftExportPackage> {
    val buildTaskName = lowerCamelCaseName(
        taskNamePrefix,
        "buildSPMPackage"
    )

    val packageBuild = locateOrRegisterTask<BuildSPMSwiftExportPackage>(buildTaskName) { task ->
        task.group = BasePlugin.BUILD_GROUP
        task.description = "Builds $taskNamePrefix SPM package"

        // Input
        task.swiftApiModuleName.set(swiftApiModuleName)
        task.swiftLibraryName.set(swiftApiLibraryName)
        task.packageRoot.set(packageGenerationTask.map { it.packagePath.get() })
        task.target.set(target.konanTarget)
        task.configuration.set(configuration)

        // Output
        task.packageBuildDir.set(layout.buildDirectory.dir("SPMBuild/${target.name}/$configuration"))
        task.packageDerivedData.set(layout.buildDirectory.dir("SPMDerivedData"))
    }

    return packageBuild
}

private fun Project.registerMergeLibraryTask(
    appleTarget: AppleTarget,
    configuration: String,
    pipelineType: SwiftExportPipelineType,
    staticLibrary: AbstractNativeLibrary,
    swiftApiModuleName: Provider<String>,
    packageBuildTask: TaskProvider<BuildSPMSwiftExportPackage>,
): TaskProvider<MergeStaticLibrariesTask> {

    val pipelineName = when (pipelineType) {
        SwiftExportPipelineType.EMBED -> "Embed"
        else -> ""
    }

    val mergeTaskName = lowerCamelCaseName(
        "merge",
        appleTarget.targetName,
        configuration,
        pipelineName,
        "SwiftExportLibraries"
    )

    val libraryName = swiftApiModuleName.map {
        lowerCamelCaseName(
            "lib",
            it,
            ".a"
        )
    }

    val mergeTask = locateOrRegisterTask<MergeStaticLibrariesTask>(mergeTaskName) { task ->
        task.description = "Merges multiple ${configuration.capitalize()} Swift Export libraries into one"

        // Output
        task.library.set(
            layout.buildDirectory.file(
                libraryName.map {
                    "MergedLibraries/${appleTarget.targetName}/$configuration/$it"
                }
            )
        )
    }

    mergeTask.configure { task ->
        task.addLibrary(staticLibrary.linkTaskProvider.map { it.outputFile.get() })
        task.addLibrary(packageBuildTask.map { it.packageLibrary.getFile() })
    }

    return mergeTask
}

