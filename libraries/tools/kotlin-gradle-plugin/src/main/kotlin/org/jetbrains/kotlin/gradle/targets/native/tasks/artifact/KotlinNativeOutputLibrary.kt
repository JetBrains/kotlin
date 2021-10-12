/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.tasks.artifact

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.BasePlugin
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeOutputKind
import org.jetbrains.kotlin.gradle.plugin.mpp.enabledOnCurrentHost
import org.jetbrains.kotlin.gradle.tasks.dependsOn
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.konan.target.presetName
import org.jetbrains.kotlin.konan.util.visibleName

class KotlinNativeOutputLibrary : KotlinNativeLibraryArtifact {
    override fun registerAssembleTask(
        project: Project,
        name: String,
        config: KotlinNativeLibraryConfig
    ) {
        val kind = if (config.isStatic) NativeOutputKind.STATIC else NativeOutputKind.DYNAMIC

        config.targets.firstOrNull { !kind.availableFor(it) }?.let { target ->
            project.logger.error("Native library '${name}' wasn't configured because ${kind.description} is not available for ${target.visibleName}")
            return
        }

        val resultTask = project.registerTask<Task>(lowerCamelCaseName("assemble", kind.taskNameClassifier, name)) { task ->
            task.group = BasePlugin.BUILD_GROUP
            task.description = "Assemble ${kind.description} '$name'."
            task.enabled = config.targets.all { it.enabledOnCurrentHost }
        }

        config.targets.forEach { target ->
            val librariesConfigurationName = project.registerLibsDependencies(target, name, config.includeDeps)
            val includeConfigurationName = project.registerIncludeDependencies(target, name, config.includeDeps)
            config.modes.forEach { buildType ->
                val targetTask = project.registerTask<KotlinNativeLinkArtifactTask>(
                    lowerCamelCaseName("assemble", buildType.visibleName, kind.taskNameClassifier, name, target.presetName),
                    listOf(target, kind.compilerOutputKind)
                ) { task ->
                    task.group = BasePlugin.BUILD_GROUP
                    task.description = "Assemble ${kind.description} '$name' for a target '${target.name}'."
                    task.enabled = target.enabledOnCurrentHost && kind.availableFor(target)

                    task.baseName = name
                    task.optimized = buildType.optimized
                    task.debuggable = buildType.debuggable
                    task.linkerOptions = config.linkerOptions
                    task.binaryOptions = config.binaryOptions

                    task.librariesConfiguration = librariesConfigurationName
                    task.includeLibrariesConfiguration = includeConfigurationName

                    task.languageSettings(config.languageSettingsFn)
                    task.kotlinOptions(config.kotlinOptionsFn)
                }
                resultTask.dependsOn(targetTask)
            }
        }
    }
}

//DSL
val KotlinNativeLibraryConfig.library: () -> KotlinNativeLibraryArtifact get() = { KotlinNativeOutputLibrary() }