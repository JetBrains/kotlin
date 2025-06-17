/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.AbstractNativeLibrary
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.AppleTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.appleTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.configuration
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal.*
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal.SwiftExportClasspathResolvableConfiguration
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal.exportedSwiftExportApiConfiguration
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal.normalizedSwiftExportModuleName
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.tasks.*
import org.jetbrains.kotlin.gradle.plugin.mpp.internal
import org.jetbrains.kotlin.gradle.tasks.locateOrRegisterTask
import org.jetbrains.kotlin.gradle.utils.*
import org.jetbrains.kotlin.konan.target.Distribution
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeAsciiOnly

internal object SwiftExportConstants {
    const val SWIFT_EXPORT_COMPILATION = "swiftExportMain"
    const val SWIFT_EXPORT_BINARY = "SwiftExportBinary"
}

internal fun Project.registerSwiftExportTask(
    swiftExportExtension: SwiftExportExtension,
    taskGroup: String,
    buildType: NativeBuildType,
    target: KotlinNativeTarget,
): TaskProvider<*> {
    val mainCompilation = target.compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME)
    val buildConfiguration = buildType.configuration

    val swiftApiModuleName = swiftExportExtension
        .moduleName
        .orElse(provider { project.name.normalizedSwiftExportModuleName.also { validateSwiftExportModuleName(it) } })

    val taskNamePrefix = lowerCamelCaseName(
        target.disambiguationClassifier ?: target.name,
        buildType.getName(),
    )

    val swiftExportTask = registerSwiftExportRun(
        taskNamePrefix = taskNamePrefix,
        taskGroup = taskGroup,
        target = target,
        configuration = buildConfiguration,
        swiftApiModuleName = swiftApiModuleName,
        exportConfiguration = target.exportedSwiftExportApiConfiguration(
            buildType,
            mainCompilation.internal.configurations.compileDependencyConfiguration
        ),
        mainCompilation = mainCompilation,
        swiftApiFlattenPackage = swiftExportExtension.flattenPackage,
        exportedModules = swiftExportExtension.exportedModules,
        customSetting = swiftExportExtension.advancedConfiguration.settings
    )

    val staticLibrary = registerSwiftExportCompilationAndGetBinary(
        buildType = buildType,
        target = target,
        mainCompilation = mainCompilation,
        freeCompilerArgs = swiftExportExtension.advancedConfiguration.freeCompilerArgs,
        swiftExportTask = swiftExportTask
    )

    swiftExportExtension.addBinary(staticLibrary)

    val swiftApiLibraryName = swiftApiModuleName.map { it + "Library" }

    val packageGenerationTask = registerPackageGeneration(
        taskNamePrefix = taskNamePrefix,
        taskGroup = taskGroup,
        target = target,
        configuration = buildConfiguration,
        swiftApiModuleName = swiftApiModuleName,
        swiftApiLibraryName = swiftApiLibraryName,
        swiftExportTask = swiftExportTask
    )
    val packageBuild = registerSPMPackageBuild(
        taskNamePrefix = taskNamePrefix,
        taskGroup = taskGroup,
        target = target,
        configuration = buildConfiguration,
        swiftApiModuleName = swiftApiModuleName,
        swiftApiLibraryName = swiftApiLibraryName,
        packageGenerationTask = packageGenerationTask
    )
    val mergeLibrariesTask = registerMergeLibraryTask(
        taskGroup = taskGroup,
        appleTarget = target.konanTarget.appleTarget,
        configuration = buildConfiguration,
        staticLibrary = staticLibrary,
        swiftApiModuleName = swiftApiModuleName,
        packageBuildTask = packageBuild
    )

    return registerCopyTask(
        taskGroup = taskGroup,
        configuration = buildConfiguration,
        libraryName = mergeLibrariesTask.map { it.library.getFile().name },
        packageGenerationTask = packageGenerationTask,
        packageBuildTask = packageBuild,
        mergeLibrariesTask = mergeLibrariesTask
    )
}

private fun Project.registerSwiftExportRun(
    taskNamePrefix: String,
    taskGroup: String,
    target: KotlinNativeTarget,
    configuration: String,
    swiftApiModuleName: Provider<String>,
    exportConfiguration: Configuration,
    mainCompilation: KotlinNativeCompilation,
    swiftApiFlattenPackage: Provider<String>,
    exportedModules: Provider<Set<SwiftExportedDependency>>,
    customSetting: Provider<Map<String, String>>,
): TaskProvider<SwiftExportTask> {
    val swiftExportTaskName = lowerCamelCaseName(
        taskNamePrefix,
        "swiftExport"
    )

    val outputs = layout.buildDirectory.dir("SwiftExport/${target.name}/$configuration")
    val files = outputs.map { it.dir("files") }
    val serializedModules = outputs.map { it.dir("modules").file("${swiftApiModuleName.get()}.json") }
    val configurationProvider = provider { LazyResolvedConfiguration(exportConfiguration) }

    return locateOrRegisterTask<SwiftExportTask>(swiftExportTaskName) { task ->
        task.description = "Run $taskNamePrefix Swift Export process"
        task.group = taskGroup

        task.inputs.files(exportConfiguration)
        task.inputs.files(mainCompilation.compileTaskProvider.map { it.outputs.files })

        // Input
        task.swiftExportClasspath.from(SwiftExportClasspathResolvableConfiguration)
        task.parameters.konanTarget.set(target.konanTarget)
        task.parameters.bridgeModuleName.set("SharedBridge")
        task.parameters.swiftExportSettings.set(customSetting)
        task.parameters.swiftModules.set(
            collectModules(
                configurationProvider,
                exportedModules
            )
        )

        task.mainModuleInput.moduleName.set(swiftApiModuleName)
        task.mainModuleInput.flattenPackage.set(swiftApiFlattenPackage)
        task.kotlinNativeProvider.set(
            mainCompilation.compileTaskProvider.flatMap { it.kotlinNativeProvider }
        )
        task.mainModuleInput.artifact.fileProvider(
            mainCompilation.compileTaskProvider.flatMap { it.outputFile }
        )

        // Output
        task.parameters.outputPath.set(files)
        task.parameters.swiftModulesFile.set(serializedModules)
    }
}

private fun registerSwiftExportCompilationAndGetBinary(
    buildType: NativeBuildType,
    target: KotlinNativeTarget,
    mainCompilation: KotlinNativeCompilation,
    freeCompilerArgs: Provider<List<String>>,
    swiftExportTask: TaskProvider<SwiftExportTask>,
): AbstractNativeLibrary {
    target.compilations.getOrCreate(
        SwiftExportConstants.SWIFT_EXPORT_COMPILATION,
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

            target.binaries.staticLib(SwiftExportConstants.SWIFT_EXPORT_BINARY, listOf(buildType)) { staticLib ->
                staticLib.compilation = swiftExportCompilation
                staticLib.binaryOption("swiftExport", "true")
                staticLib.binaryOption("cInterfaceMode", "none")

                staticLib.linkTaskProvider.configure {
                    it.toolOptions.freeCompilerArgs.addAll(freeCompilerArgs)
                }
            }
        }
    )

    return target.binaries.getStaticLib(
        SwiftExportConstants.SWIFT_EXPORT_BINARY,
        buildType
    )
}

private fun Project.registerPackageGeneration(
    taskNamePrefix: String,
    taskGroup: String,
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

    return locateOrRegisterTask<GenerateSPMPackageFromSwiftExport>(spmPackageGenTaskName) { task ->
        task.description = "Generates $taskNamePrefix SPM Package"
        task.group = taskGroup

        // Input
        task.kotlinRuntime.set(
            file(Distribution(konanDistribution.root.absolutePath).kotlinRuntimeForSwiftHome)
        )

        task.swiftModulesFile.set(swiftExportTask.map { it.parameters.swiftModulesFile.get() })
        task.swiftLibraryName.set(swiftApiLibraryName)
        task.swiftApiModuleName.set(swiftApiModuleName)

        // Output
        task.packagePath.set(layout.buildDirectory.dir("SPMPackage/${target.name}/$configuration"))
    }
}

private fun Project.registerSPMPackageBuild(
    taskNamePrefix: String,
    taskGroup: String,
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

    return locateOrRegisterTask<BuildSPMSwiftExportPackage>(buildTaskName) { task ->
        task.description = "Builds $taskNamePrefix SPM package"
        task.group = taskGroup

        // Input
        task.configuration.set(configuration)
        task.packageRoot.set(packageGenerationTask.flatMap { it.packagePath })
        task.swiftApiModuleName.set(swiftApiModuleName)
        task.swiftLibraryName.set(swiftApiLibraryName)
        task.target.set(target.konanTarget)

        // Output
        task.packageBuildDir.set(layout.buildDirectory.dir("SPMBuild/${target.name}/$configuration"))
        task.packageDerivedData.set(layout.buildDirectory.dir("SPMDerivedData"))
    }
}

private fun Project.registerMergeLibraryTask(
    taskGroup: String,
    appleTarget: AppleTarget,
    configuration: String,
    staticLibrary: AbstractNativeLibrary,
    swiftApiModuleName: Provider<String>,
    packageBuildTask: TaskProvider<BuildSPMSwiftExportPackage>,
): TaskProvider<MergeStaticLibrariesTask> {

    val mergeTaskName = lowerCamelCaseName(
        "merge",
        appleTarget.targetName,
        configuration,
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
        task.description = "Merges multiple ${configuration.capitalizeAsciiOnly()} Swift Export libraries into one"
        task.group = taskGroup

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

private fun Project.registerCopyTask(
    taskGroup: String,
    configuration: String,
    libraryName: Provider<String>,
    packageGenerationTask: TaskProvider<GenerateSPMPackageFromSwiftExport>,
    packageBuildTask: TaskProvider<BuildSPMSwiftExportPackage>,
    mergeLibrariesTask: TaskProvider<MergeStaticLibrariesTask>,
): TaskProvider<out Task> {

    val copyTaskName = lowerCamelCaseName(
        "copy",
        configuration,
        "SPMIntermediates"
    )

    val copyTask = locateOrRegisterTask<CopySwiftExportIntermediatesForConsumer>(copyTaskName) { task ->
        task.description = "Copy ${configuration.capitalizeAsciiOnly()} SPM intermediates"
        task.group = taskGroup

        // Input
        task.includes.from(packageGenerationTask.map { it.includesPath.get() })
        task.libraryName.set(libraryName)
        task.library.set(mergeLibrariesTask.map { it.library.get() })
    }

    copyTask.configure { task ->
        task.addInterface(
            packageBuildTask.map { it.interfacesPath.asFile.get() }
        )
    }

    return copyTask
}