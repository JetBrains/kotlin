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
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.tasks.*
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

internal object SwiftExportConstants {
    const val SWIFT_EXPORT_COMPILATION = "swiftExportMain"
    const val SWIFT_EXPORT_BINARY = "SwiftExportBinary"
}

internal fun Project.registerSwiftExportTask(
    framework: Framework,
): TaskProvider<*> {
    val mainCompilation = framework.target.compilations.getByName("main")
    val buildConfiguration = framework.buildType.configuration
    val target = framework.target
    val swiftApiModuleName = provider {
        framework.baseNameProvider.getOrElse(dashSeparatedToUpperCamelCase(project.name))
    }

    val taskNamePrefix = lowerCamelCaseName(
        target.disambiguationClassifier ?: target.name,
        framework.buildType.getName(),
    )

    val swiftExportTask = registerSwiftExportRun(
        taskNamePrefix = taskNamePrefix,
        binary = framework,
        configuration = buildConfiguration,
        mainCompilation = mainCompilation,
        swiftApiModuleName = swiftApiModuleName,
    )

    val staticLibrary = registerSwiftExportCompilationAndGetBinary(
        buildType = framework.buildType,
        compilations = target.compilations,
        binaries = target.binaries,
        mainCompilation = mainCompilation,
        swiftExportTask = swiftExportTask
    )

    val swiftApiLibraryName = swiftApiModuleName.map { it + "Library" }

    val packageGenerationTask = registerPackageGeneration(
        taskNamePrefix = taskNamePrefix,
        target = target,
        configuration = buildConfiguration,
        swiftApiModuleName = swiftApiModuleName,
        swiftApiLibraryName = swiftApiLibraryName,
        swiftExportTask = swiftExportTask
    )
    val packageBuild = registerSPMPackageBuild(
        taskNamePrefix = taskNamePrefix,
        target = target,
        configuration = buildConfiguration,
        swiftApiModuleName = swiftApiModuleName,
        swiftApiLibraryName = swiftApiLibraryName,
        packageGenerationTask = packageGenerationTask
    )
    val mergeLibrariesTask = registerMergeLibraryTask(
        appleTarget = target.konanTarget.appleTarget,
        configuration = buildConfiguration,
        staticLibrary = staticLibrary,
        swiftApiModuleName = swiftApiModuleName,
        packageBuildTask = packageBuild
    )

    return registerCopyTask(
        configuration = buildConfiguration,
        libraryName = mergeLibrariesTask.map { it.library.getFile().name },
        packageGenerationTask = packageGenerationTask,
        packageBuildTask = packageBuild,
        mergeLibrariesTask = mergeLibrariesTask
    )
}

private fun Project.registerSwiftExportRun(
    taskNamePrefix: String,
    binary: Framework,
    configuration: String,
    mainCompilation: KotlinNativeCompilation,
    swiftApiModuleName: Provider<String>,
): TaskProvider<SwiftExportTask> {
    val swiftExportTaskName = lowerCamelCaseName(
        taskNamePrefix,
        "swiftExport"
    )

    val outputs = layout.buildDirectory.dir("SwiftExport/${binary.target.name}/$configuration")
    val files = outputs.map { it.dir("files") }
    val serializedModules = outputs.map { it.dir("modules").file("${swiftApiModuleName.get()}.json") }
    val compileConfiguration = mainCompilation.internal.configurations.compileDependencyConfiguration
    val configurationProvider = provider { LazyResolvedConfiguration(compileConfiguration) }

    return locateOrRegisterTask<SwiftExportTask>(swiftExportTaskName) { task ->
        task.description = "Run $taskNamePrefix Swift Export process"

        task.inputs.files(compileConfiguration)

        // Input
        task.swiftExportClasspath.from(maybeCreateSwiftExportClasspathResolvableConfiguration())
        task.parameters.bridgeModuleName.set("SharedBridge")
        task.parameters.konanDistribution.set(Distribution(konanDistribution.root.absolutePath))

        task.configuration.set(configurationProvider)
        task.mainModuleName.set(swiftApiModuleName)
        task.mainArtifact.set(
            objects.fileProperty().fileProvider(
                mainCompilation.compileTaskProvider.map { it.outputFile.get() }
            )
        )

        // Output
        task.parameters.outputPath.set(files)
        task.parameters.swiftModulesFile.set(serializedModules)
    }
}

private fun registerSwiftExportCompilationAndGetBinary(
    buildType: NativeBuildType,
    compilations: NamedDomainObjectContainer<KotlinNativeCompilation>,
    binaries: KotlinNativeBinaryContainer,
    mainCompilation: KotlinNativeCompilation,
    swiftExportTask: TaskProvider<SwiftExportTask>,
): AbstractNativeLibrary {
    compilations.getOrCreate(
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

            binaries.staticLib(SwiftExportConstants.SWIFT_EXPORT_BINARY) { staticLib ->
                staticLib.compilation = swiftExportCompilation
                staticLib.binaryOption("swiftExport", "true")
                staticLib.binaryOption("cInterfaceMode", "none")
            }
        }
    )

    return binaries.getStaticLib(
        SwiftExportConstants.SWIFT_EXPORT_BINARY,
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

    return locateOrRegisterTask<GenerateSPMPackageFromSwiftExport>(spmPackageGenTaskName) { task ->
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

    return locateOrRegisterTask<BuildSPMSwiftExportPackage>(buildTaskName) { task ->
        task.description = "Builds $taskNamePrefix SPM package"
        task.group = BasePlugin.BUILD_GROUP

        // Input
        task.configuration.set(configuration)
        task.packageRoot.set(packageGenerationTask.map { it.packagePath.get() })
        task.swiftApiModuleName.set(swiftApiModuleName)
        task.swiftLibraryName.set(swiftApiLibraryName)
        task.target.set(target.konanTarget)

        // Output
        task.packageBuildDir.set(layout.buildDirectory.dir("SPMBuild/${target.name}/$configuration"))
        task.packageDerivedData.set(layout.buildDirectory.dir("SPMDerivedData"))
    }
}

private fun Project.registerMergeLibraryTask(
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
        task.description = "Merges multiple ${configuration.capitalize()} Swift Export libraries into one"
        task.group = BasePlugin.BUILD_GROUP

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
        task.description = "Copy ${configuration.capitalize()} SPM intermediates"
        task.group = BasePlugin.BUILD_GROUP

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