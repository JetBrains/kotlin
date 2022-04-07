/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.tasks.artifact

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.plugins.ExtensionAware
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonToolOptions
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeOutputKind
import org.jetbrains.kotlin.gradle.plugin.mpp.enabledOnCurrentHost
import org.jetbrains.kotlin.gradle.tasks.dependsOn
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.target.presetName
import org.jetbrains.kotlin.konan.util.visibleName
import javax.inject.Inject

abstract class KotlinNativeLibraryConfig @Inject constructor(
    artifactName: String
) : KotlinNativeArtifactConfig(artifactName) {
    abstract var target: KonanTarget

    override fun createArtifact(project: Project, extensions: ExtensionAware) = KotlinNativeLibrary(
        project,
        artifactName,
        modules,
        modes,
        isStatic,
        linkerOptions,
        kotlinOptionsFn,
        binaryOptions,
        target,
        extensions
    )
}

class KotlinNativeLibrary(
    override val project: Project,
    override val artifactName: String,
    override val modules: Set<Any>,
    override val modes: Set<NativeBuildType>,
    override val isStatic: Boolean,
    override val linkerOptions: List<String>,
    override val kotlinOptionsFn: KotlinCommonToolOptions.() -> Unit,
    override val binaryOptions: Map<String, String>,
    val target: KonanTarget,
    extensions: ExtensionAware
) : KotlinNativeArtifact, ExtensionAware by extensions {
    private val kind = if (isStatic) NativeOutputKind.STATIC else NativeOutputKind.DYNAMIC
    override fun getName() = lowerCamelCaseName(artifactName, kind.taskNameClassifier, "Library", target.presetName)

    override fun validate() {
        super.validate()
        check(kind.availableFor(target)) {
            "Native artifact '$artifactName' wasn't configured because ${kind.description} is not available for ${target.visibleName}"
        }
    }

    override fun registerAssembleTask() {
        validate()
        val resultTask = project.registerTask<Task>(taskName) { task ->
            task.group = BasePlugin.BUILD_GROUP
            task.description = "Assemble all types of registered '$artifactName' ${kind.description} for ${target.visibleName}."
            task.enabled = target.enabledOnCurrentHost
        }
        project.tasks.named(LifecycleBasePlugin.ASSEMBLE_TASK_NAME).dependsOn(resultTask)

        val librariesConfigurationName = project.registerLibsDependencies(target, artifactName, modules)
        val exportConfigurationName = project.registerExportDependencies(target, artifactName, modules)
        modes.forEach { buildType ->
            val targetTask = project.registerTask<KotlinNativeLinkArtifactTask>(
                lowerCamelCaseName("assemble", artifactName, buildType.visibleName, kind.taskNameClassifier, "Library", target.presetName),
                listOf(target, kind.compilerOutputKind)
            ) { task ->
                task.description = "Assemble ${kind.description} '$artifactName' for a target '${target.name}'."
                task.enabled = target.enabledOnCurrentHost
                task.baseName = artifactName
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