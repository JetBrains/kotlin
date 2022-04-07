/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.tasks.artifact

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.ExtensionAware
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonToolOptions
import org.jetbrains.kotlin.gradle.plugin.mpp.BitcodeEmbeddingMode
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeOutputKind
import org.jetbrains.kotlin.gradle.tasks.FatFrameworkTask
import org.jetbrains.kotlin.gradle.tasks.FrameworkDescriptor
import org.jetbrains.kotlin.gradle.tasks.dependsOn
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.util.visibleName
import javax.inject.Inject

abstract class KotlinNativeFatFrameworkConfig @Inject constructor(
    artifactName: String
) : KotlinNativeArtifactConfig(artifactName) {
    var targets: Set<KonanTarget> = emptySet()
    fun targets(vararg targets: KonanTarget) {
        this.targets = targets.toSet()
    }

    var embedBitcode: BitcodeEmbeddingMode? = null

    override fun createArtifact(project: Project, extensions: ExtensionAware) = KotlinNativeFatFramework(
        project,
        artifactName,
        modules,
        modes,
        isStatic,
        linkerOptions,
        kotlinOptionsFn,
        binaryOptions,
        targets,
        embedBitcode,
        extensions
    )
}

class KotlinNativeFatFramework(
    override val project: Project,
    override val artifactName: String,
    override val modules: Set<Any>,
    override val modes: Set<NativeBuildType>,
    override val isStatic: Boolean,
    override val linkerOptions: List<String>,
    override val kotlinOptionsFn: KotlinCommonToolOptions.() -> Unit,
    override val binaryOptions: Map<String, String>,
    val targets: Set<KonanTarget>,
    val embedBitcode: BitcodeEmbeddingMode?,
    extensions: ExtensionAware
) : KotlinNativeArtifact, ExtensionAware by extensions {
    private val kind = NativeOutputKind.FRAMEWORK
    override fun getName() = lowerCamelCaseName(artifactName, "FatFramework")

    override fun validate() {
        super.validate()
        check(targets.isNotEmpty()) {
            "Native artifact '$artifactName' wasn't configured because it requires at least one target"
        }
        val wrongTarget = targets.firstOrNull { !kind.availableFor(it) }
        check(wrongTarget == null) {
            "Native artifact '$artifactName' wasn't configured because ${kind.description} is not available for ${wrongTarget!!.visibleName}"
        }
    }

    override fun registerAssembleTask() {
        validate()
        val parentTask = project.registerTask<Task>(taskName) {
            it.group = "build"
            it.description = "Assemble all types of registered '$artifactName' FatFramework"
        }
        project.tasks.named(LifecycleBasePlugin.ASSEMBLE_TASK_NAME).dependsOn(parentTask)

        modes.forEach { buildType ->
            val fatTask = project.registerTask<FatFrameworkTask>(
                lowerCamelCaseName("assemble", artifactName, buildType.visibleName, "FatFramework")
            ) {
                it.baseName = artifactName
                it.destinationDir = project.buildDir.resolve("out/fatframework/${buildType.getName()}")
            }
            parentTask.dependsOn(fatTask)

            val nameSuffix = "ForFat"
            val frameworkDescriptors: List<FrameworkDescriptor> = targets.map { target ->
                val librariesConfigurationName = project.registerLibsDependencies(target, artifactName + nameSuffix, modules)
                val exportConfigurationName = project.registerExportDependencies(target, artifactName + nameSuffix, modules)
                val targetTask = registerLinkFrameworkTask(
                    project,
                    artifactName,
                    target,
                    buildType,
                    librariesConfigurationName,
                    exportConfigurationName,
                    embedBitcode,
                    "${artifactName}FatFrameworkTemp",
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