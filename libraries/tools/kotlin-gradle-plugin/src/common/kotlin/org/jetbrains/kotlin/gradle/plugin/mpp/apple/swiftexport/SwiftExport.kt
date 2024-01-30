/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.KotlinNativeBinaryContainer
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.kotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.launch
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.tasks.dependsOn
import org.jetbrains.kotlin.gradle.tasks.locateOrRegisterTask
import org.jetbrains.kotlin.gradle.utils.future
import org.jetbrains.kotlin.gradle.utils.getOrCreate
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.gradle.utils.mapToFile

fun Project.registerSwiftExportTask(
    framework: Framework
): TaskProvider<*> {
    return registerSwiftExportTask(
        swiftApiModuleName = framework.baseName,
        target = framework.target,
        buildType = framework.buildType,
    )
}

private fun Project.registerSwiftExportTask(
    swiftApiModuleName: String,
    target: KotlinNativeTarget,
    buildType: NativeBuildType,
): TaskProvider<*> {
    val taskNamePrefix = lowerCamelCaseName(
        target.disambiguationClassifier ?: target.name,
        buildType.getName(),
    )
    val mainCompilation = target.compilations.getByName("main")

    val swiftExportTask = registerSwiftExportRun(
        swiftApiModuleName = swiftApiModuleName,
        taskNamePrefix = taskNamePrefix,
        mainCompilation = mainCompilation,
    )
    val staticLibrary = registerSwiftExportCompilationAndGetBinary(
        buildType = buildType,
        compilations = target.compilations,
        binaries = target.binaries,
        mainCompilation = mainCompilation,
        swiftExportTask = swiftExportTask,
    )

    val kotlinStaticLibraryName = staticLibrary.linkTaskProvider.map { it.baseName }
    val swiftApiLibraryName = swiftApiModuleName + "Library"

    val syntheticBuildRoot = layout.buildDirectory.dir("${taskNamePrefix}SPMPackage")
    val packageGenerationTask = registerPackageGeneration(
        taskNamePrefix = taskNamePrefix,
        swiftApiModuleName = swiftApiModuleName,
        swiftApiLibraryName = swiftApiLibraryName,
        kotlinStaticLibraryName = kotlinStaticLibraryName,
        swiftExportTask = swiftExportTask,
        staticLibrary = staticLibrary,
        syntheticBuildRoot = syntheticBuildRoot,
    )
    val syntheticProjectBuild = registerSyntheticProjectBuild(
        taskNamePrefix = taskNamePrefix,
        swiftApiModuleName = swiftApiModuleName,
        swiftApiLibraryName = swiftApiLibraryName,
        syntheticBuildRoot = syntheticBuildRoot,
        packageGenerationTask = packageGenerationTask,
    )

    return registerCopyTask(
        taskNamePrefix = taskNamePrefix,
        staticLibrary = staticLibrary,
        packageGenerationTask = packageGenerationTask,
        syntheticProjectBuild = syntheticProjectBuild,
    )
}

private fun Project.registerSwiftExportRun(
    swiftApiModuleName: String,
    taskNamePrefix: String,
    mainCompilation: KotlinCompilation<*>,
): TaskProvider<SwiftExportTask> {
    val swiftExportTaskName = taskNamePrefix + "SwiftExport"
    return locateOrRegisterTask<SwiftExportTask>(swiftExportTaskName) { task ->
        val directoriesProvider = project.future {
            kotlinPluginLifecycle.await(KotlinPluginLifecycle.Stage.AfterFinaliseRefinesEdges)
            mainCompilation.allKotlinSourceSets.flatMap { sourceSet ->
                sourceSet.kotlin.srcDirs.filter { it.exists() }
            }
        }
        // Explicitly depend on source sets directories because there is no @InputDirectories annotation
        project.launch {
            directoriesProvider.await().forEach {
                task.inputs.dir(it)
            }
        }
        val outputs = layout.buildDirectory.dir("${swiftExportTaskName}SwiftExport")
        val swiftIntermediates = outputs.map { it.dir("swiftIntermediates") }
        val kotlinIntermediates = outputs.map { it.dir("kotlinIntermediates") }

        // Input
        task.sourceRoots.from(project.files(project.provider { directoriesProvider.getOrThrow() }))
        task.bridgeModuleName.set("${swiftApiModuleName}Bridge")

        // Output
        task.swiftApiPath.set(swiftIntermediates.map { it.file("KotlinAPI.swift") })
        task.headerBridgePath.set(swiftIntermediates.map { it.file("KotlinBridge.h") })
        task.kotlinBridgePath.set(kotlinIntermediates.map { it.file("KotlinBridge.kt") })
    }
}

private fun registerSwiftExportCompilationAndGetBinary(
    buildType: NativeBuildType,
    compilations: NamedDomainObjectContainer<KotlinNativeCompilation>,
    binaries: KotlinNativeBinaryContainer,
    mainCompilation: KotlinCompilation<*>,
    swiftExportTask: TaskProvider<SwiftExportTask>,
): StaticLibrary {
    val swiftExportCompilationName = "swiftExportMain"
    val swiftExportBinary = "swiftExportBinary"

    compilations.getOrCreate(
        swiftExportCompilationName,
        invokeWhenCreated = { swiftExportCompilation ->
            swiftExportCompilation.associateWith(mainCompilation)
            swiftExportCompilation.compileTaskProvider.dependsOn(swiftExportTask)
            swiftExportCompilation.defaultSourceSet.kotlin.srcDir(swiftExportTask.map { it.kotlinBridgePath.get().asFile.parent })
            swiftExportCompilation.compilerOptions.options.optIn.add("kotlin.experimental.ExperimentalNativeApi")

            binaries.staticLib(swiftExportBinary) { staticLib ->
                staticLib.compilation = swiftExportCompilation
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
    swiftApiModuleName: String,
    swiftApiLibraryName: String,
    kotlinStaticLibraryName: Provider<String>,
    swiftExportTask: TaskProvider<SwiftExportTask>,
    staticLibrary: StaticLibrary,
    syntheticBuildRoot: Provider<Directory>,
): TaskProvider<GenerateSPMPackageFromSwiftExport> {
    val spmPackageGenTaskName = taskNamePrefix + "GenerateSPMPackage"
    val packageGenerationTask = locateOrRegisterTask<GenerateSPMPackageFromSwiftExport>(spmPackageGenTaskName) { task ->
        task.group = BasePlugin.BUILD_GROUP
        task.description = "Generates $taskNamePrefix SPM Package"

        // Input
        task.swiftApiPath.set(swiftExportTask.map { it.swiftApiPath.get() })
        task.headerBridgePath.set(swiftExportTask.map { it.headerBridgePath.get() })
        task.headerBridgeModuleName.set(swiftExportTask.map { it.bridgeModuleName.get() })
        task.libraryPath.set { staticLibrary.linkTaskProvider.map { it.outputFile.get() }.get() }
        task.swiftLibraryName.set(swiftApiLibraryName)
        task.kotlinLibraryName.set(kotlinStaticLibraryName)
        task.swiftApiModuleName.set(swiftApiModuleName)

        // Output
        task.packagePath.set(syntheticBuildRoot.map { it.dir(swiftApiModuleName) })
    }
    packageGenerationTask.dependsOn(staticLibrary.linkTaskProvider)
    packageGenerationTask.dependsOn(swiftExportTask)
    return packageGenerationTask
}

private fun Project.registerSyntheticProjectBuild(
    taskNamePrefix: String,
    swiftApiModuleName: String,
    swiftApiLibraryName: String,
    syntheticBuildRoot: Provider<Directory>,
    packageGenerationTask: TaskProvider<*>,
): TaskProvider<BuildSyntheticProjectWithSwiftExportPackage> {
    val buildTaskName = taskNamePrefix + "BuildSyntheticProject"
    val syntheticProjectBuild = locateOrRegisterTask<BuildSyntheticProjectWithSwiftExportPackage>(buildTaskName) { task ->
        task.group = BasePlugin.BUILD_GROUP
        task.description = "Builds $taskNamePrefix synthetic project"
        task.swiftApiModuleName.set(swiftApiModuleName)
        task.swiftLibraryName.set(swiftApiLibraryName)
        task.syntheticProjectDirectory.set(syntheticBuildRoot)
    }
    syntheticProjectBuild.dependsOn(packageGenerationTask)
    return syntheticProjectBuild
}

private fun Project.registerCopyTask(
    taskNamePrefix: String,
    staticLibrary: StaticLibrary,
    packageGenerationTask: TaskProvider<GenerateSPMPackageFromSwiftExport>,
    syntheticProjectBuild: TaskProvider<BuildSyntheticProjectWithSwiftExportPackage>,
): TaskProvider<*> {
    val copyTaskName = taskNamePrefix + "CopySyntheticProjectIntermediates"
    val copyTask = locateOrRegisterTask<CopySwiftExportIntermediatesForConsumer>(copyTaskName) { task ->
        task.group = BasePlugin.BUILD_GROUP
        task.description = "Copy $taskNamePrefix synthetic project intermediates"
        task.includeBridgeDirectory.convention(layout.file(packageGenerationTask.map { it.headerBridgeIncludePath }))
        task.kotlinLibraryPath.convention(layout.file(staticLibrary.linkTaskProvider.flatMap { it.outputFile }))
        task.syntheticLibraryPath.convention(layout.file(syntheticProjectBuild.flatMap { it.syntheticLibraryPath.mapToFile() }))
        task.syntheticInterfacesPath.convention(layout.file(syntheticProjectBuild.flatMap { it.syntheticInterfacesPath.mapToFile() }))
    }
    copyTask.dependsOn(syntheticProjectBuild)
    return copyTask
}