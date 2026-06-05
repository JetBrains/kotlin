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
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.AppleTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.tasks.*
import org.jetbrains.kotlin.gradle.tasks.dependsOn
import org.jetbrains.kotlin.gradle.tasks.locateOrRegisterTask
import org.jetbrains.kotlin.gradle.utils.getFile
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeAsciiOnly

/**
 * Registers the merge-modules and assemble-xcframework tasks for Swift Export xcframework publishing.
 * Called from [registerSwiftExportFrameworkTask] after the common pipeline tasks are set up.
 */
internal fun Project.registerSwiftExportXCFrameworkPipeline(
    swiftApiModuleName: Provider<String>,
    configuration: String,
    appleTarget: AppleTarget,
    packageGenerationTask: TaskProvider<GenerateSPMPackageFromSwiftExport>,
    packageBuildTask: TaskProvider<BuildSPMSwiftExportPackage>,
    mergeLibrariesTask: TaskProvider<MergeStaticLibrariesTask>,
): TaskProvider<out Task> {
    val mergeModulesTask = registerMergeSwiftModulesTask(
        appleTarget = appleTarget,
        configuration = configuration,
        packageBuildTask = packageBuildTask,
    )

    return registerProduceSwiftExportFrameworkTask(
        configuration = configuration,
        swiftApiModuleName = swiftApiModuleName,
        packageGenerationTask = packageGenerationTask,
        mergeLibrariesTask = mergeLibrariesTask,
        mergeModulesTask = mergeModulesTask,
    )
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
        task.description = "Merges multiple ${configuration.capitalizeAsciiOnly()} Swift Export modules into one"

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
        task.description = "Creates Swift Export ${configuration.capitalizeAsciiOnly()} Apple XCFramework"

        task.binaryName.set(swiftApiModuleName)
        task.includes.set(packageGenerationTask.map { it.includesPath.get() })
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
