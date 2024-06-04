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
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.tasks.BuildSPMSwiftExportPackage
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.tasks.CopySwiftExportIntermediatesForConsumer
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.tasks.GenerateSPMPackageFromSwiftExport
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.tasks.MergeStaticLibrariesTask
import org.jetbrains.kotlin.gradle.tasks.locateOrRegisterTask
import org.jetbrains.kotlin.gradle.utils.getFile
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName

internal fun Project.registerSwiftExportEmbedPipelineTask(
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
        pipelineType = SwiftExportPipelineType.EMBED
    ) { configuration, packageBuild, packageGenerationTask, mergeLibrariesTask ->
        registerCopyTask(
            configuration = configuration,
            libraryName = mergeLibrariesTask.map { it.library.getFile().name },
            packageGenerationTask = packageGenerationTask,
            packageBuildTask = packageBuild,
            mergeLibrariesTask = mergeLibrariesTask
        )
    }
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
        task.group = BasePlugin.BUILD_GROUP
        task.description = "Copy ${configuration.capitalize()} SPM intermediates"

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


