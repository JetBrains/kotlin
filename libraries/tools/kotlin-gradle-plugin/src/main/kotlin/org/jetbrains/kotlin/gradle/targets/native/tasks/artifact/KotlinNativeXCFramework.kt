/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.tasks.artifact

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.jetbrains.kotlin.gradle.plugin.mpp.BitcodeEmbeddingMode
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeOutputKind
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.AppleTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFrameworkTaskHolder
import org.jetbrains.kotlin.gradle.tasks.FrameworkDescriptor
import org.jetbrains.kotlin.gradle.tasks.dependsOn
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.konan.target.KonanTarget

class KotlinNativeXCFramework : KotlinNativeArtifact() {
    var targets: Set<KonanTarget> = emptySet()
    var embedBitcode: BitcodeEmbeddingMode? = null

    private val kind = NativeOutputKind.FRAMEWORK

    override fun validate(project: Project, name: String): Boolean {
        val logger = project.logger
        if (!super.validate(project, name)) return false
        if (targets.isEmpty()) {
            logger.error("Native library '${name}' wasn't configured because it requires at least one target")
            return false
        }
        targets.firstOrNull { !kind.availableFor(it) }?.let { target ->
            logger.error("Native library '${name}' wasn't configured because ${kind.description} is not available for ${target.visibleName}")
            return false
        }

        return true
    }

    override fun registerAssembleTask(project: Project, name: String) {
        val parentTask = project.registerTask<Task>(lowerCamelCaseName("assemble", name, "XCFramework")) {
            it.group = "build"
            it.description = "Assemble all types of registered '$name' XCFramework"
        }
        project.tasks.named(LifecycleBasePlugin.ASSEMBLE_TASK_NAME).dependsOn(parentTask)

        modes.forEach { buildType ->
            val holder = XCFrameworkTaskHolder.create(project, name, buildType).also {
                parentTask.dependsOn(it.task)
            }

            val nameSuffix = "ForXCF"
            val frameworkDescriptors: List<FrameworkDescriptor> = targets.map { target ->
                val librariesConfigurationName = project.registerLibsDependencies(target, name + nameSuffix, modules)
                val exportConfigurationName = project.registerExportDependencies(target, name + nameSuffix, modules)
                val targetTask = registerLinkFrameworkTask(
                    project,
                    name,
                    target,
                    buildType,
                    librariesConfigurationName,
                    exportConfigurationName,
                    embedBitcode,
                    "${name}XCFrameworkTemp",
                    nameSuffix
                )
                holder.task.dependsOn(targetTask)
                val frameworkFileProvider = targetTask.map { it.outputFile }
                val descriptor = FrameworkDescriptor(frameworkFileProvider.get(), isStatic, target)

                val group = AppleTarget.values().firstOrNull { it.targets.contains(target) }
                holder.fatTasks[group]?.configure { fatTask ->
                    fatTask.fromFrameworkDescriptors(listOf(descriptor))
                    fatTask.dependsOn(targetTask)
                }
                descriptor
            }
            holder.task.configure {
                it.fromFrameworkDescriptors(frameworkDescriptors)
                it.outputDir = project.buildDir.resolve("out/xcframework")
            }
        }
    }
}