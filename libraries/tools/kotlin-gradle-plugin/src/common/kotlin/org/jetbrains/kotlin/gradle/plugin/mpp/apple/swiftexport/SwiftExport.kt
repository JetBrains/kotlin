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
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.tasks.dependsOn
import org.jetbrains.kotlin.gradle.tasks.locateOrRegisterTask
import org.jetbrains.kotlin.gradle.utils.*
import org.jetbrains.kotlin.gradle.utils.future
import org.jetbrains.kotlin.gradle.utils.getOrCreate
import org.jetbrains.kotlin.gradle.utils.konanDistribution
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.gradle.utils.mapToFile

fun Project.registerSwiftExportTask(
    framework: Framework,
): TaskProvider<*> {
    return registerSwiftExportTask(
        swiftApiModuleName = framework.baseNameProvider,
        target = framework.target,
        buildType = framework.buildType,
    )
}

private fun Project.registerSwiftExportTask(
    swiftApiModuleName: Provider<String>,
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

    val kotlinStaticLibraryName = staticLibrary.linkTaskProvider.flatMap { it.binary.baseNameProvider }
    val swiftApiLibraryName = swiftApiModuleName.map { it + "Library" }

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
    swiftApiModuleName: Provider<String>,
    taskNamePrefix: String,
    mainCompilation: KotlinCompilation<*>,
): TaskProvider<SwiftExportTask> {
    val swiftExportTaskName = taskNamePrefix + "SwiftExport"
    return locateOrRegisterTask<SwiftExportTask>(swiftExportTaskName) { task ->
        val directoryProvider = project.future {
            kotlinPluginLifecycle.await(KotlinPluginLifecycle.Stage.AfterFinaliseRefinesEdges)
            mainCompilation.allKotlinSourceSets.flatMap {
                it.kotlin.srcDirs
            }
        }

        val outputs = layout.buildDirectory.dir(swiftExportTaskName)
        val swiftIntermediates = outputs.map { it.dir("swiftIntermediates") }
        val kotlinIntermediates = outputs.map { it.dir("kotlinIntermediates") }

        // Input
        task.sourceRoots.from(project.files(directoryProvider.getOrThrow()))
        task.bridgeModuleName.set(swiftApiModuleName.map { "${it}Bridge" })
        task.debugMode.set(true)
        task.konanDistribution.set(konanDistribution.root)

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
            swiftExportCompilation.defaultSourceSet.kotlin.srcDir(swiftExportTask.map { it.kotlinBridgePath.getFile().parent })
            swiftExportCompilation.compileTaskProvider.configure {
                it.compilerOptions.optIn.add("kotlin.experimental.ExperimentalNativeApi")
            }

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
    swiftApiModuleName: Provider<String>,
    swiftApiLibraryName: Provider<String>,
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
        task.swiftApiPath.set(swiftExportTask.flatMap { it.swiftApiPath })
        task.headerBridgePath.set(swiftExportTask.flatMap { it.headerBridgePath })
        task.headerBridgeModuleName.set(swiftExportTask.flatMap { it.bridgeModuleName })
        task.libraryPath.set { staticLibrary.linkTaskProvider.flatMap { it.outputFile }.get() }
        task.swiftLibraryName.set(swiftApiLibraryName)
        task.kotlinLibraryName.set(kotlinStaticLibraryName)
        task.swiftApiModuleName.set(swiftApiModuleName)

        // Output
        task.packagePath.set(syntheticBuildRoot.flatMap { root ->
            swiftApiModuleName.map { root.dir(it) }
        })
    }

    packageGenerationTask.dependsOn(staticLibrary.linkTaskProvider)
    return packageGenerationTask
}

private fun Project.registerSyntheticProjectBuild(
    taskNamePrefix: String,
    swiftApiModuleName: Provider<String>,
    swiftApiLibraryName: Provider<String>,
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