/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.AppleTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.appleTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.tasks.*
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.tasks.BuildSPMSwiftExportPackage
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.tasks.GenerateSPMPackageFromSwiftExport
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.tasks.SwiftExportXCFrameworkTask
import org.jetbrains.kotlin.gradle.tasks.dependsOn
import org.jetbrains.kotlin.gradle.tasks.locateOrRegisterTask
import org.jetbrains.kotlin.gradle.utils.getFile
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName

internal fun Project.registerSwiftExportFrameworkPipelineTask(
    swiftApiModuleName: Provider<String>,
    taskNamePrefix: String,
    target: KotlinNativeTarget,
    buildType: NativeBuildType,
): TaskProvider<out Task> {
    return setupCommonSwiftExportPipeline(
        swiftApiModuleName = swiftApiModuleName,
        taskNamePrefix = taskNamePrefix,
        target = target,
        buildType = buildType,
        pipelineType = SwiftExportPipelineType.FRAMEWORK
    ) { configuration, packageBuild, packageGenerationTask, mergeLibrariesTask ->

        val mergeModulesTask = registerMergeSwiftModulesTask(
            appleTarget = target.konanTarget.appleTarget,
            configuration = configuration,
            packageBuildTask = packageBuild
        )

        registerProduceSwiftExportFrameworkTask(
            configuration = configuration,
            swiftApiModuleName = swiftApiModuleName,
            packageGenerationTask = packageGenerationTask,
            mergeLibrariesTask = mergeLibrariesTask,
            mergeModulesTask = mergeModulesTask,
        )
    }
}

private fun Project.registerMergeSwiftModulesTask(
    appleTarget: AppleTarget,
    configuration: String,
    packageBuildTask: TaskProvider<BuildSPMSwiftExportPackage>,
): TaskProvider<MergeSwiftModulesTask> {
    val mergeTaskName = lowerCamelCaseName(
        "merge",
        appleTarget.targetName,
        configuration,
        "SwiftExportModules"
    )

    val mergeTask = locateOrRegisterTask<MergeSwiftModulesTask>(mergeTaskName) { task ->
        task.description = "Merges multiple ${configuration.capitalize()} Swift Export modules into one"

        // Output
        task.modules.set(
            layout.buildDirectory.dir("MergedModules/${appleTarget.targetName}/$configuration")
        )
    }

    mergeTask.configure { task ->
        task.addInterface(
            packageBuildTask.map { it.interfacesPath.getFile() }
        )
    }

    return mergeTask
}

private fun Project.registerProduceSwiftExportFrameworkTask(
    configuration: String,
    swiftApiModuleName: Provider<String>,
    packageGenerationTask: TaskProvider<GenerateSPMPackageFromSwiftExport>,
    mergeLibrariesTask: TaskProvider<MergeStaticLibrariesTask>,
    mergeModulesTask: TaskProvider<MergeSwiftModulesTask>,
): TaskProvider<out Task> {
    val frameworkTaskName = lowerCamelCaseName(
        "assemble",
        configuration,
        "swiftExportFramework"
    )

    val frameworkTask = locateOrRegisterTask<SwiftExportXCFrameworkTask>(frameworkTaskName) { task ->
        task.group = BasePlugin.BUILD_GROUP
        task.description = "Creates Swift Export $configuration Apple Framework"

        // Input
        task.binaryName.set(swiftApiModuleName)
        task.includes.set(packageGenerationTask.map { it.includesPath.get() })

        // Output
        task.frameworkRoot.set(layout.buildDirectory.dir("SwiftExportFramework/$configuration"))
    }

    frameworkTask.configure { task ->
        task.addLibrary(provider {
            LibraryDefinition(
                mergeLibrariesTask.map { it.library.getFile() }.get(),
                mergeModulesTask.map { it.modules.getFile() }.get()
            )
        })
    }

    frameworkTask.dependsOn(mergeLibrariesTask)
    frameworkTask.dependsOn(mergeModulesTask)
    frameworkTask.dependsOn(packageGenerationTask)

    return frameworkTask
}