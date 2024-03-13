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
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.FrameworkTask
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

    val packageBuildRoot = layout.buildDirectory.dir("${taskNamePrefix}SPMPackage")
    val packageGenerationTask = registerPackageGeneration(
        taskNamePrefix = taskNamePrefix,
        swiftApiModuleName = swiftApiModuleName,
        swiftApiLibraryName = swiftApiLibraryName,
        kotlinStaticLibraryName = kotlinStaticLibraryName,
        swiftExportTask = swiftExportTask,
        staticLibrary = staticLibrary,
        packageBuildRoot = packageBuildRoot,
    )
    val packageBuild = registerSPMPackageBuild(
        taskNamePrefix = taskNamePrefix,
        swiftApiModuleName = swiftApiModuleName,
        swiftApiLibraryName = swiftApiLibraryName,
        packageBuildRoot = packageBuildRoot,
        packageGenerationTask = packageGenerationTask,
    )

    val frameworkTask = registerFrameworkTask(
        taskNamePrefix = taskNamePrefix,
        swiftApiModuleName = swiftApiModuleName,
        frameworkRoot = packageBuildRoot,
        swiftExportTask = swiftExportTask,
        packageBuildTask = packageBuild
    )

    return registerCopyTask(
        taskNamePrefix = taskNamePrefix,
        staticLibrary = staticLibrary,
        packageGenerationTask = packageGenerationTask,
        packageBuildTask = packageBuild,
        frameworkTask = frameworkTask
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
    packageBuildRoot: Provider<Directory>,
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
        task.packagePath.set(packageBuildRoot.flatMap { root ->
            swiftApiModuleName.map { root.dir(it) }
        })
    }

    packageGenerationTask.dependsOn(staticLibrary.linkTaskProvider)
    return packageGenerationTask
}

private fun Project.registerSPMPackageBuild(
    taskNamePrefix: String,
    swiftApiModuleName: Provider<String>,
    swiftApiLibraryName: Provider<String>,
    packageBuildRoot: Provider<Directory>,
    packageGenerationTask: TaskProvider<*>,
): TaskProvider<BuildSPMSwiftExportPackage> {
    val buildTaskName = taskNamePrefix + "BuildSPMPackage"
    val packageBuild = locateOrRegisterTask<BuildSPMSwiftExportPackage>(buildTaskName) { task ->
        task.group = BasePlugin.BUILD_GROUP
        task.description = "Builds $taskNamePrefix SPM package"
        task.swiftApiModuleName.set(swiftApiModuleName)
        task.swiftLibraryName.set(swiftApiLibraryName)
        task.packageBuildDirectory.convention(packageBuildRoot)
    }
    packageBuild.dependsOn(packageGenerationTask)
    return packageBuild
}

private fun Project.registerFrameworkTask(
    taskNamePrefix: String,
    swiftApiModuleName: Provider<String>,
    frameworkRoot: Provider<Directory>,
    swiftExportTask: TaskProvider<SwiftExportTask>,
    packageBuildTask: TaskProvider<BuildSPMSwiftExportPackage>,
): TaskProvider<FrameworkTask> {
    val frameworkTaskName = taskNamePrefix + "Framework"
    val createFramework = locateOrRegisterTask<FrameworkTask>(frameworkTaskName) { task ->

        val header = swiftExportTask.flatMap { it.parameters.headerBridgePath }
        val konanHeader = layout.file(
            provider {
                file(Distribution(konanDistribution.root.absolutePath).kotlinRuntimeForSwiftHeader)
            }
        )

        task.group = BasePlugin.BUILD_GROUP
        task.description = "Creates $taskNamePrefix Apple Framework"
        task.frameworkPath.set(frameworkRoot)
        task.frameworkName.set(swiftApiModuleName)
        task.bundleIdentifier.set(swiftApiModuleName.map { "com.jetbrains.$it" })
        task.binary.set(packageBuildTask.flatMap { it.packageLibraryPath })
        task.headers.from(header, konanHeader)
    }
    createFramework.dependsOn(packageBuildTask)
    return createFramework
}

private fun Project.registerCopyTask(
    taskNamePrefix: String,
    staticLibrary: StaticLibrary,
    packageGenerationTask: TaskProvider<GenerateSPMPackageFromSwiftExport>,
    packageBuildTask: TaskProvider<BuildSPMSwiftExportPackage>,
    frameworkTask: TaskProvider<FrameworkTask>,
): TaskProvider<*> {
    val copyTaskName = taskNamePrefix + "CopySPMIntermediates"
    val copyTask = locateOrRegisterTask<CopySwiftExportIntermediatesForConsumer>(copyTaskName) { task ->
        task.group = BasePlugin.BUILD_GROUP
        task.description = "Copy $taskNamePrefix SPM intermediates"
        task.includeBridgeDirectory.set(layout.file(packageGenerationTask.map { it.headerBridgeIncludePath }))
        task.includeKotlinRuntimeDirectory.set(layout.file(packageGenerationTask.map { it.kotlinRuntimeIncludePath }))
        task.kotlinLibraryPath.set(layout.file(staticLibrary.linkTaskProvider.flatMap { it.outputFile }))
        task.packageLibraryPath.set(layout.file(packageBuildTask.flatMap { it.packageLibraryPath.mapToFile() }))
        task.packageInterfacesPath.set(layout.file(packageBuildTask.flatMap { it.interfacesPath.mapToFile() }))
        task.frameworkPath.set(layout.file(frameworkTask.flatMap { it.frameworkRootPath.mapToFile() }))
    }
    copyTask.dependsOn(packageBuildTask)
    copyTask.dependsOn(frameworkTask)
    return copyTask
}

