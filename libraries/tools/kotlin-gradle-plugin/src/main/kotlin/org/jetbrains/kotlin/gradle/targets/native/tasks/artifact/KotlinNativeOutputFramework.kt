/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.tasks.artifact

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.BitcodeEmbeddingMode
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeOutputKind
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.*
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.AppleTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFrameworkTaskHolder
import org.jetbrains.kotlin.gradle.plugin.mpp.enabledOnCurrentHost
import org.jetbrains.kotlin.gradle.tasks.dependsOn
import org.jetbrains.kotlin.gradle.tasks.locateOrRegisterTask
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.target.presetName
import org.jetbrains.kotlin.konan.util.visibleName

class KotlinNativeFrameworkConfig {
    var embedBitcode: BitcodeEmbeddingMode? = null
}

private class KotlinNativeOutputFramework(
    val frameworkConfig: KotlinNativeFrameworkConfig
) : KotlinNativeLibraryArtifact {
    override fun registerAssembleTask(
        project: Project,
        name: String,
        config: KotlinNativeLibraryConfig
    ) {
        val kind = NativeOutputKind.FRAMEWORK

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
            val librariesConfigurationName = project.registerLibsDependencies(target, name, config.exportDeps)
            val exportConfigurationName = project.registerExportDependencies(target, name, config.exportDeps)
            config.modes.forEach { buildType ->
                val targetTask = project.registerLinkFrameworkTask(
                    lowerCamelCaseName("assemble", buildType.visibleName, kind.taskNameClassifier, name, target.presetName),
                    target,
                    buildType,
                    config,
                    librariesConfigurationName,
                    exportConfigurationName,
                    frameworkConfig.embedBitcode
                )
                resultTask.dependsOn(targetTask)
            }
        }
    }
}

private class KotlinNativeOutputXCFramework(
    val frameworkConfig: KotlinNativeFrameworkConfig
) : KotlinNativeLibraryArtifact {
    override fun registerAssembleTask(
        project: Project,
        name: String,
        config: KotlinNativeLibraryConfig
    ) {
        val kind = NativeOutputKind.FRAMEWORK

        config.targets.firstOrNull { !kind.availableFor(it) }?.let { target ->
            project.logger.error("Native library '${name}' wasn't configured because ${kind.description} is not available for ${target.visibleName}")
            return
        }

        val parentTask = project.registerTask<Task>(lowerCamelCaseName("assemble", name, "XCFramework")) {
            it.group = "build"
            it.description = "Assemble all types of registered '$name' XCFramework"
        }
        config.modes.forEach { buildType ->
            val holder = XCFrameworkTaskHolder.create(project, name, buildType).also {
                parentTask.dependsOn(it.task)
            }
            config.targets.forEach { target ->
                val librariesConfigurationName = project.registerLibsDependencies(target, name, config.exportDeps)
                val exportConfigurationName = project.registerExportDependencies(target, name, config.exportDeps)
                project.registerLinkFrameworkTask(
                    lowerCamelCaseName("assemble", buildType.visibleName, kind.taskNameClassifier, name, target.presetName),
                    target,
                    buildType,
                    config,
                    librariesConfigurationName,
                    exportConfigurationName,
                    frameworkConfig.embedBitcode
                ).configure { targetTask ->
                    holder.task.configure { xcFrameworkTask ->
                        xcFrameworkTask.from(target, targetTask.outputFile, targetTask.isStaticFramework)
                        xcFrameworkTask.dependsOn(targetTask)
                    }
//                    val group = AppleTarget.values().firstOrNull { it.targets.contains(target) }
//                            holder.fatTasks[group]?.configure { fatTask ->
//                                fatTask.from(framework)
//                            }
                }
            }
        }
    }
}

private fun Project.registerLinkFrameworkTask(
    name: String,
    target: KonanTarget,
    buildType: NativeBuildType,
    config: KotlinNativeLibraryConfig,
    librariesConfigurationName: String,
    exportConfigurationName: String,
    embedBitcode: BitcodeEmbeddingMode?
): TaskProvider<KotlinNativeLinkArtifactTask> {
    val kind = NativeOutputKind.FRAMEWORK
    return registerTask(name, listOf(target, kind.compilerOutputKind)) { task ->
        task.group = BasePlugin.BUILD_GROUP
        task.description = "Assemble ${kind.description} '$name' for a target '${target.name}'."
        task.enabled = target.enabledOnCurrentHost && kind.availableFor(target)

        task.baseName = name
        task.optimized = buildType.optimized
        task.debuggable = buildType.debuggable
        task.linkerOptions = config.linkerOptions
        task.binaryOptions = config.binaryOptions

        task.isStaticFramework = config.isStatic
        task.embedBitcode = embedBitcode ?: buildType.embedBitcode(target)

        task.librariesConfiguration = librariesConfigurationName
        task.exportLibrariesConfiguration = exportConfigurationName

        task.languageSettings(config.languageSettingsFn)
        task.kotlinOptions(config.kotlinOptionsFn)
    }
}

//DSL
val KotlinNativeLibraryConfig.framework: () -> KotlinNativeLibraryArtifact get() = framework {}
fun KotlinNativeLibraryConfig.framework(
    fn: KotlinNativeFrameworkConfig.() -> Unit
): () -> KotlinNativeLibraryArtifact = {
    val frameworkConfig = KotlinNativeFrameworkConfig()
    frameworkConfig.fn()
    KotlinNativeOutputFramework(frameworkConfig)
}

val KotlinNativeLibraryConfig.xcframework: () -> KotlinNativeLibraryArtifact get() = xcframework {}
fun KotlinNativeLibraryConfig.xcframework(
    fn: KotlinNativeFrameworkConfig.() -> Unit
): () -> KotlinNativeLibraryArtifact = {
    val frameworkConfig = KotlinNativeFrameworkConfig()
    frameworkConfig.fn()
    KotlinNativeOutputXCFramework(frameworkConfig)
}