/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.tasks.artifact

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.ExtensionAware
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.jetbrains.kotlin.gradle.dsl.*
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

abstract class KotlinNativeFatFrameworkConfigImpl @Inject constructor(artifactName: String) :
    KotlinNativeArtifactConfigImpl(artifactName), KotlinNativeFatFrameworkConfig {
    override var targets: Set<KonanTarget> = emptySet()
    override fun targets(vararg targets: KonanTarget) {
        this.targets = targets.toSet()
    }

    override var embedBitcode: BitcodeEmbeddingMode? = null

    override fun validate() {
        super.validate()
        val kind = NativeOutputKind.FRAMEWORK
        check(targets.isNotEmpty()) {
            "Native artifact '$artifactName' wasn't configured because it requires at least one target"
        }
        val wrongTarget = targets.firstOrNull { !kind.availableFor(it) }
        check(wrongTarget == null) {
            "Native artifact '$artifactName' wasn't configured because ${kind.description} is not available for ${wrongTarget!!.visibleName}"
        }
    }

    override fun createArtifact(extensions: ExtensionAware): KotlinNativeFatFrameworkImpl {
        validate()
        return KotlinNativeFatFrameworkImpl(
            artifactName = artifactName,
            modules = modules,
            modes = modes,
            isStatic = isStatic,
            linkerOptions = linkerOptions,
            kotlinOptionsFn = kotlinOptionsFn,
            toolOptionsConfigure = toolOptionsConfigure,
            binaryOptions = binaryOptions,
            targets = targets,
            embedBitcode = embedBitcode,
            extensions = extensions
        )
    }
}

class KotlinNativeFatFrameworkImpl(
    override val artifactName: String,
    override val modules: Set<Any>,
    override val modes: Set<NativeBuildType>,
    override val isStatic: Boolean,
    override val linkerOptions: List<String>,
    override val kotlinOptionsFn: KotlinCommonToolOptions.() -> Unit,
    override val toolOptionsConfigure: KotlinCommonCompilerToolOptions.() -> Unit,
    override val binaryOptions: Map<String, String>,
    override val targets: Set<KonanTarget>,
    override val embedBitcode: BitcodeEmbeddingMode?,
    extensions: ExtensionAware
) : KotlinNativeFatFramework, ExtensionAware by extensions {
    override fun getName() = lowerCamelCaseName(artifactName, "FatFramework")
    override val taskName = lowerCamelCaseName("assemble", name)
    override val outDir
        get() = "out/fatframework"

    override fun registerAssembleTask(project: Project) {
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
                it.destinationDirProperty.set(project.layout.buildDirectory.dir("$outDir/${buildType.getName()}"))
            }
            parentTask.dependsOn(fatTask)

            val nameSuffix = "ForFat"
            val frameworkDescriptors: List<FrameworkDescriptor> = targets.map { target ->
                val librariesConfigurationName = project.registerLibsDependencies(target, artifactName + nameSuffix, modules)
                val exportConfigurationName = project.registerExportDependencies(target, artifactName + nameSuffix, modules)
                val targetTask = registerLinkFrameworkTask(
                    project = project,
                    name = artifactName,
                    target = target,
                    buildType = buildType,
                    librariesConfigurationName = librariesConfigurationName,
                    exportConfigurationName = exportConfigurationName,
                    embedBitcode = embedBitcode,
                    outDirName = "${artifactName}FatFrameworkTemp",
                    taskNameSuffix = nameSuffix
                )
                fatTask.dependsOn(targetTask)
                val frameworkFileProvider = targetTask.flatMap { it.outputFile }
                FrameworkDescriptor(frameworkFileProvider.get(), isStatic, target)
            }
            fatTask.configure { it.fromFrameworkDescriptors(frameworkDescriptors) }
        }
    }
}