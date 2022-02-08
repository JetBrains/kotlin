/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.tasks.artifact

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.BasePlugin
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeOutputKind
import org.jetbrains.kotlin.gradle.plugin.mpp.enabledOnCurrentHost
import org.jetbrains.kotlin.gradle.tasks.dependsOn
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.target.presetName
import org.jetbrains.kotlin.konan.util.visibleName

open class KotlinNativeLibrary : KotlinNativeArtifact() {
    lateinit var target: KonanTarget

    private val kind: NativeOutputKind
        get() = if (isStatic) NativeOutputKind.STATIC else NativeOutputKind.DYNAMIC

    override fun validate(project: Project, name: String): Boolean {
        val logger = project.logger
        if (!super.validate(project, name)) return false
        if (!this::target.isInitialized) {
            logger.error("Native library '${name}' wasn't configured because it requires target")
            return false
        }
        if (!kind.availableFor(target)) {
            logger.error("Native library '${name}' wasn't configured because ${kind.description} is not available for ${target.visibleName}")
            return false
        }

        return true
    }

    override fun registerAssembleTask(project: Project, name: String) {

        val resultTask = project.registerTask<Task>(
            lowerCamelCaseName("assemble", name, kind.taskNameClassifier, "Library")
        ) { task ->
            task.group = BasePlugin.BUILD_GROUP
            task.description = "Assemble all types of registered '$name' ${kind.description}."
            task.enabled = target.enabledOnCurrentHost
        }
        project.tasks.named(LifecycleBasePlugin.ASSEMBLE_TASK_NAME).dependsOn(resultTask)

        val librariesConfigurationName = project.registerLibsDependencies(target, name, modules)
        val exportConfigurationName = project.registerExportDependencies(target, name, modules)
        modes.forEach { buildType ->
            val targetTask = project.registerTask<KotlinNativeLinkArtifactTask>(
                lowerCamelCaseName("assemble", name, buildType.visibleName, kind.taskNameClassifier, "Library", target.presetName),
                listOf(target, kind.compilerOutputKind)
            ) { task ->
                task.description = "Assemble ${kind.description} '$name' for a target '${target.name}'."
                task.enabled = target.enabledOnCurrentHost
                task.baseName = name
                task.optimized = buildType.optimized
                task.debuggable = buildType.debuggable
                task.linkerOptions = linkerOptions
                task.binaryOptions = binaryOptions
                task.librariesConfiguration = librariesConfigurationName
                task.exportLibrariesConfiguration = exportConfigurationName
                task.kotlinOptions(kotlinOptionsFn)
            }
            resultTask.dependsOn(targetTask)
        }
    }
}