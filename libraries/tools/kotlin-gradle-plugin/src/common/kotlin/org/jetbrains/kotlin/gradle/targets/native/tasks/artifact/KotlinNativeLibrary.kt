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
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeOutputKind
import org.jetbrains.kotlin.gradle.plugin.mpp.enabledOnCurrentHostForBinariesCompilation
import org.jetbrains.kotlin.gradle.targets.native.toolchain.chooseKotlinNativeProvider
import org.jetbrains.kotlin.gradle.tasks.dependsOn
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.target.presetName
import org.jetbrains.kotlin.konan.util.visibleName
import javax.inject.Inject

@Deprecated(KotlinArtifactsExtension.KOTLIN_NATIVE_ARTIFACTS_DEPRECATION)
abstract class KotlinNativeLibraryConfigImpl @Inject constructor(artifactName: String) :
    KotlinNativeArtifactConfigImpl(artifactName), KotlinNativeLibraryConfig {

    override fun validate() {
        super.validate()
        val kind = if (isStatic) NativeOutputKind.STATIC else NativeOutputKind.DYNAMIC
        check(kind.availableFor(target)) {
            "Native artifact '$artifactName' wasn't configured because ${kind.description} is not available for ${target.visibleName}"
        }
    }

    override fun createArtifact(extensions: ExtensionAware): KotlinNativeLibraryImpl {
        validate()
        return KotlinNativeLibraryImpl(
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
class KotlinNativeLibraryImpl(
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
    extensions: ExtensionAware,
) : KotlinNativeLibrary, ExtensionAware by extensions {
    private val kind = if (isStatic) NativeOutputKind.STATIC else NativeOutputKind.DYNAMIC
    override fun getName() = lowerCamelCaseName(artifactName, kind.taskNameClassifier, "Library", target.presetName)
    override val taskName = lowerCamelCaseName("assemble", name)
    override val outDir = "out/${kind.visibleName}"

    override fun registerAssembleTask(project: Project) {
        val resultTask = project.registerTask<Task>(taskName) { task ->
            task.group = BasePlugin.BUILD_GROUP
            task.description = "Assemble all types of registered '$artifactName' ${kind.description} for ${target.visibleName}."
            task.enabled = target.enabledOnCurrentHostForBinariesCompilation
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
                task.destinationDir.set(project.layout.buildDirectory.dir("$outDir/${target.visibleName}/${buildType.visibleName}"))
                val enabledOnCurrentHost = target.enabledOnCurrentHostForBinariesCompilation
                task.enabled = enabledOnCurrentHost
                task.baseName.set(artifactName)
                task.optimized.set(buildType.optimized)
                task.debuggable.set(buildType.debuggable)
                task.linkerOptions.set(linkerOptions)
                task.binaryOptions.set(binaryOptions)
                task.libraries.setFrom(project.configurations.getByName(librariesConfigurationName))
                task.exportLibraries.setFrom(project.configurations.getByName(exportConfigurationName))
                @Suppress("DEPRECATION_ERROR")
                task.kotlinOptions(kotlinOptionsFn)
                task.toolOptions(toolOptionsConfigure)
                task.kotlinNativeProvider.set(task.chooseKotlinNativeProvider(enabledOnCurrentHost, task.konanTarget))
                task.kotlinCompilerArgumentsLogLevel
                    .value(project.kotlinPropertiesProvider.kotlinCompilerArgumentsLogLevel)
                    .finalizeValueOnRead()
            }
            resultTask.dependsOn(targetTask)
        }
    }
}
