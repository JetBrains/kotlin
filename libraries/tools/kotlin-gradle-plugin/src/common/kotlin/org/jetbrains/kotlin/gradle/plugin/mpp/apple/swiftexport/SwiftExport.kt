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
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformSourceSetConventionsImpl.commonMain
import org.jetbrains.kotlin.gradle.dsl.KotlinNativeBinaryContainer
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.tasks.dependsOn
import org.jetbrains.kotlin.gradle.tasks.locateOrRegisterTask
import org.jetbrains.kotlin.gradle.utils.*
import org.jetbrains.kotlin.gradle.utils.future
import org.jetbrains.kotlin.gradle.utils.getOrCreate
import org.jetbrains.kotlin.gradle.utils.konanDistribution
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.gradle.utils.mapToFile
import org.jetbrains.kotlin.konan.target.Distribution

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
): TaskProvider<SwiftExportTask> {
    val swiftExportTaskName = taskNamePrefix + "SwiftExport"

    return locateOrRegisterTask<SwiftExportTask>(swiftExportTaskName) { task ->
        val commonMainProvider = project.future {
            project
                .multiplatformExtension
                .awaitSourceSets()
                .commonMain
                .get()
                .kotlin
                .srcDirs
                .single()
        }

        val outputs = layout.buildDirectory.dir(swiftExportTaskName)
        val swiftIntermediates = outputs.map { it.dir("swiftIntermediates") }
        val kotlinIntermediates = outputs.map { it.dir("kotlinIntermediates") }

        // Input
        task.swiftExportClasspath.from(maybeCreateSwiftExportClasspathResolvableConfiguration())
        task.parameters.sourceRoot.convention(commonMainProvider.map { objects.directoryProperty(it) }.getOrThrow())
        task.parameters.swiftApiModuleName.convention(swiftApiModuleName)
        task.parameters.bridgeModuleName.convention(swiftApiModuleName.map { "${it}Bridge" })
        task.parameters.debugMode.convention(true)
        task.parameters.konanDistribution.convention(Distribution(konanDistribution.root.absolutePath))

        // Output
        task.parameters.swiftApiPath.convention(swiftIntermediates.map { it.file("KotlinAPI.swift") })
        task.parameters.headerBridgePath.convention(swiftIntermediates.map { it.file("KotlinBridge.h") })
        task.parameters.kotlinBridgePath.convention(kotlinIntermediates.map { it.file("KotlinBridge.kt") })
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
            swiftExportCompilation.defaultSourceSet.kotlin.srcDir(swiftExportTask.map {
                it.parameters.kotlinBridgePath.getFile().parent
            })

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
        task.kotlinRuntime.convention(
            objects.directoryProperty(
                file(Distribution(konanDistribution.root.absolutePath).kotlinRuntimeForSwiftHome)
            )
        )

        task.swiftApiPath.convention(swiftExportTask.flatMap { it.parameters.swiftApiPath })
        task.headerBridgePath.convention(swiftExportTask.flatMap { it.parameters.headerBridgePath })
        task.headerBridgeModuleName.convention(swiftExportTask.flatMap { it.parameters.bridgeModuleName })
        task.libraryPath.convention(staticLibrary.linkTaskProvider.flatMap { layout.file(it.outputFile) })
        task.swiftLibraryName.convention(swiftApiLibraryName)
        task.kotlinLibraryName.convention(kotlinStaticLibraryName)
        task.swiftApiModuleName.convention(swiftApiModuleName)

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
): TaskProvider<BuildSPMSwiftExportPackage> {
    val buildTaskName = taskNamePrefix + "BuildSPMPackage"
    val syntheticProjectBuild = locateOrRegisterTask<BuildSPMSwiftExportPackage>(buildTaskName) { task ->
        task.group = BasePlugin.BUILD_GROUP
        task.description = "Builds $taskNamePrefix SPM package"
        task.swiftApiModuleName.set(swiftApiModuleName)
        task.swiftLibraryName.set(swiftApiLibraryName)
        task.syntheticProjectDirectory.convention(syntheticBuildRoot)
    }
    syntheticProjectBuild.dependsOn(packageGenerationTask)
    return syntheticProjectBuild
}

private fun Project.registerCopyTask(
    taskNamePrefix: String,
    staticLibrary: StaticLibrary,
    packageGenerationTask: TaskProvider<GenerateSPMPackageFromSwiftExport>,
    syntheticProjectBuild: TaskProvider<BuildSPMSwiftExportPackage>,
): TaskProvider<*> {
    val copyTaskName = taskNamePrefix + "CopySPMIntermediates"
    val copyTask = locateOrRegisterTask<CopySwiftExportIntermediatesForConsumer>(copyTaskName) { task ->
        task.group = BasePlugin.BUILD_GROUP
        task.description = "Copy $taskNamePrefix SPM intermediates"
        task.includeBridgeDirectory.convention(layout.file(packageGenerationTask.map { it.headerBridgeIncludePath }))
        task.includeKotlinRuntimeDirectory.convention(layout.file(packageGenerationTask.map { it.kotlinRuntimeIncludePath }))
        task.kotlinLibraryPath.convention(layout.file(staticLibrary.linkTaskProvider.flatMap { it.outputFile }))
        task.syntheticLibraryPath.convention(layout.file(syntheticProjectBuild.flatMap { it.syntheticLibraryPath.mapToFile() }))
        task.syntheticInterfacesPath.convention(layout.file(syntheticProjectBuild.flatMap { it.syntheticInterfacesPath.mapToFile() }))
    }
    copyTask.dependsOn(syntheticProjectBuild)
    return copyTask
}

