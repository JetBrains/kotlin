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
import org.jetbrains.kotlin.gradle.tasks.FatFrameworkTask
import org.jetbrains.kotlin.gradle.tasks.FrameworkDescriptor
import org.jetbrains.kotlin.gradle.tasks.dependsOn
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.util.visibleName

class KotlinNativeFatFramework : KotlinNativeArtifact() {
    var targets: Set<KonanTarget> = emptySet()
    fun targets(vararg targets: KonanTarget) {
        this.targets = targets.toSet()
    }

    @JvmField var embedBitcode: BitcodeEmbeddingMode? = null

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
        val parentTask = project.registerTask<Task>(lowerCamelCaseName("assemble", name, "FatFramework")) {
            it.group = "build"
            it.description = "Assemble all types of registered '$name' FatFramework"
        }
        project.tasks.named(LifecycleBasePlugin.ASSEMBLE_TASK_NAME).dependsOn(parentTask)

        modes.forEach { buildType ->
            val fatTask = project.registerTask<FatFrameworkTask>(
                lowerCamelCaseName("assemble", name, buildType.visibleName, "FatFramework")
            ) {
                it.baseName = name
                it.destinationDir = project.buildDir.resolve("out/fatframework/${buildType.getName()}")
            }
            parentTask.dependsOn(fatTask)

            val nameSuffix = "ForFat"
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
                    "${name}FatFrameworkTemp",
                    nameSuffix
                )
                fatTask.dependsOn(targetTask)
                val frameworkFileProvider = targetTask.map { it.outputFile }
                FrameworkDescriptor(frameworkFileProvider.get(), isStatic, target)
            }
            fatTask.configure { it.fromFrameworkDescriptors(frameworkDescriptors) }
        }
    }
}