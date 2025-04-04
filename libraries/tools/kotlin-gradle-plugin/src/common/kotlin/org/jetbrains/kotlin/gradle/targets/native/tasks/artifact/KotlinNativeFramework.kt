/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:Suppress("DEPRECATION")

package org.jetbrains.kotlin.gradle.targets.native.tasks.artifact

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.tasks.TaskProvider
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.targets.native.toolchain.chooseKotlinNativeProvider
import org.jetbrains.kotlin.gradle.tasks.dependsOn
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.target.presetName
import org.jetbrains.kotlin.konan.util.visibleName
import javax.inject.Inject

abstract class KotlinNativeFrameworkConfigImpl @Inject constructor(artifactName: String) :
    KotlinNativeArtifactConfigImpl(artifactName), KotlinNativeFrameworkConfig {

    override fun validate() {
        super.validate()
        val kind = NativeOutputKind.FRAMEWORK
        check(kind.availableFor(target)) {
            "Native artifact '$artifactName' wasn't configured because ${kind.description} is not available for ${target.visibleName}"
        }
    }

    override fun createArtifact(extensions: ExtensionAware): KotlinNativeFrameworkImpl {
        validate()
        return KotlinNativeFrameworkImpl(
            artifactName = artifactName,
            modules = modules,
            modes = modes,
            isStatic = isStatic,
            linkerOptions = linkerOptions,
            kotlinOptionsFn = kotlinOptionsFn,
            toolOptionsConfigure = toolOptionsConfigure,
            binaryOptions = binaryOptions,
            target = target,
            extensions = extensions
        )
    }
}

@Deprecated(KotlinArtifactsExtension.KOTLIN_NATIVE_ARTIFACTS_DEPRECATION)
class KotlinNativeFrameworkImpl(
    override val artifactName: String,
    override val modules: Set<Any>,
    override val modes: Set<NativeBuildType>,
    override val isStatic: Boolean,
    override val linkerOptions: List<String>,
    @Suppress("DEPRECATION_ERROR")
    @Deprecated(
        message = "Please migrate to toolOptionsConfigure DSL. More details are here: https://kotl.in/u1r8ln",
        level = DeprecationLevel.ERROR,
    )
    override val kotlinOptionsFn: KotlinCommonToolOptions.() -> Unit,
    override val toolOptionsConfigure: KotlinCommonCompilerToolOptions.() -> Unit,
    override val binaryOptions: Map<String, String>,
    override val target: KonanTarget,
    @Suppress("DEPRECATION_ERROR")
    @Deprecated(BITCODE_EMBEDDING_DEPRECATION_MESSAGE, level = DeprecationLevel.ERROR)
    override val embedBitcode: BitcodeEmbeddingMode? = null,
    extensions: ExtensionAware
) : KotlinNativeFramework, ExtensionAware by extensions {
    private val kind = NativeOutputKind.FRAMEWORK
    override fun getName() = lowerCamelCaseName(artifactName, kind.taskNameClassifier, target.presetName)
    override val taskName = lowerCamelCaseName("assemble", name)
    override val outDir = "out/${kind.visibleName}"

    override fun registerAssembleTask(project: Project) {
        val resultTask = project.registerTask<Task>(taskName) { task ->
            task.group = BasePlugin.BUILD_GROUP
            task.description = "Assemble ${kind.description} '$artifactName' for ${target.visibleName}."
            task.enabled = target.enabledOnCurrentHostForBinariesCompilation
        }

        val librariesConfigurationName = project.registerLibsDependencies(target, artifactName, modules)
        val exportConfigurationName = project.registerExportDependencies(target, artifactName, modules)
        modes.forEach { buildType ->
            val targetTask = registerLinkFrameworkTask(
                project = project,
                name = artifactName,
                target = target,
                buildType = buildType,
                librariesConfigurationName = librariesConfigurationName,
                exportConfigurationName = exportConfigurationName,
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
    outDirName: String = outDir,
    taskNameSuffix: String = ""
): TaskProvider<KotlinNativeLinkArtifactTask> {
    val kind = NativeOutputKind.FRAMEWORK
    val destinationDir = project.layout.buildDirectory.dir("$outDirName/${target.visibleName}/${buildType.visibleName}")
    val resultTask = project.registerTask<KotlinNativeLinkArtifactTask>(
        lowerCamelCaseName("assemble", name, buildType.visibleName, kind.taskNameClassifier, target.presetName, taskNameSuffix),
        listOf(target, kind.compilerOutputKind)
    ) { task ->
        task.description = "Assemble ${kind.description} '$name' for a target '${target.name}'."
        val enabledOnCurrentHost = target.enabledOnCurrentHostForBinariesCompilation
        task.enabled = enabledOnCurrentHost
        task.baseName.set(name)
        task.destinationDir.set(destinationDir)
        task.optimized.set(buildType.optimized)
        task.debuggable.set(buildType.debuggable)
        task.linkerOptions.set(linkerOptions)
        task.binaryOptions.set(binaryOptions)
        task.staticFramework.set(isStatic)
        task.libraries.setFrom(project.configurations.getByName(librariesConfigurationName))
        task.exportLibraries.setFrom(project.configurations.getByName(exportConfigurationName))
        @Suppress("DEPRECATION_ERROR")
        task.kotlinOptions(kotlinOptionsFn)
        task.kotlinNativeProvider.set(task.chooseKotlinNativeProvider(enabledOnCurrentHost, task.konanTarget))
        task.kotlinCompilerArgumentsLogLevel
            .value(project.kotlinPropertiesProvider.kotlinCompilerArgumentsLogLevel)
            .finalizeValueOnRead()
    }
    project.tasks.named(LifecycleBasePlugin.ASSEMBLE_TASK_NAME).dependsOn(resultTask)
    return resultTask
}
