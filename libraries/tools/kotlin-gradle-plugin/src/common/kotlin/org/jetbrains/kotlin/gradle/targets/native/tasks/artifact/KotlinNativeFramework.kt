/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.tasks.artifact

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.tasks.TaskProvider
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.jetbrains.kotlin.gradle.plugin.mpp.BitcodeEmbeddingMode
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

open class KotlinNativeFramework @Inject constructor(project: Project, artifactName: String) : KotlinNativeArtifact(project, artifactName) {
    lateinit var target: KonanTarget
    var embedBitcode: BitcodeEmbeddingMode? = null

    private val kind = NativeOutputKind.FRAMEWORK

    override val taskName get() = lowerCamelCaseName("assemble", artifactName, kind.taskNameClassifier, target.presetName)

    override fun validate() {
        super.validate()
        check(this::target.isInitialized) {
            "Native artifact '$artifactName' wasn't configured because it requires target"
        }
        check(kind.availableFor(target)) {
            "Native artifact '$artifactName' wasn't configured because ${kind.description} is not available for ${target.visibleName}"
        }
    }

    override fun registerAssembleTask() {
        validate()
        val resultTask = project.registerTask<Task>(taskName) { task ->
            task.group = BasePlugin.BUILD_GROUP
            task.description = "Assemble ${kind.description} '$artifactName' for ${target.visibleName}."
            task.enabled = target.enabledOnCurrentHost
        }

        val librariesConfigurationName = project.registerLibsDependencies(target, artifactName, modules)
        val exportConfigurationName = project.registerExportDependencies(target, artifactName, modules)
        modes.forEach { buildType ->
            val targetTask = registerLinkFrameworkTask(
                project,
                artifactName,
                target,
                buildType,
                librariesConfigurationName,
                exportConfigurationName,
                embedBitcode
            )
            resultTask.dependsOn(targetTask)
        }
    }
}

internal fun KotlinNativeArtifact.registerLinkFrameworkTask(
    project: Project,
    name: String,
    target: KonanTarget,
    buildType: NativeBuildType,
    librariesConfigurationName: String,
    exportConfigurationName: String,
    embedBitcode: BitcodeEmbeddingMode?,
    outDirName: String = "out",
    taskNameSuffix: String = ""
): TaskProvider<KotlinNativeLinkArtifactTask> {
    val kind = NativeOutputKind.FRAMEWORK
    val destinationDir = project.buildDir.resolve("$outDirName/${kind.visibleName}/${target.visibleName}/${buildType.visibleName}")
    val resultTask = project.registerTask<KotlinNativeLinkArtifactTask>(
        lowerCamelCaseName("assemble", name, buildType.visibleName, kind.taskNameClassifier, target.presetName, taskNameSuffix),
        listOf(target, kind.compilerOutputKind)
    ) { task ->
        task.description = "Assemble ${kind.description} '$name' for a target '${target.name}'."
        task.enabled = target.enabledOnCurrentHost
        task.baseName = name
        task.destinationDir = destinationDir
        task.optimized = buildType.optimized
        task.debuggable = buildType.debuggable
        task.linkerOptions = linkerOptions
        task.binaryOptions = binaryOptions
        task.isStaticFramework = isStatic
        task.embedBitcode = embedBitcode ?: buildType.embedBitcode(target)
        task.librariesConfiguration = librariesConfigurationName
        task.exportLibrariesConfiguration = exportConfigurationName
        task.kotlinOptions(kotlinOptionsFn)
    }
    project.tasks.named(LifecycleBasePlugin.ASSEMBLE_TASK_NAME).dependsOn(resultTask)
    return resultTask
}