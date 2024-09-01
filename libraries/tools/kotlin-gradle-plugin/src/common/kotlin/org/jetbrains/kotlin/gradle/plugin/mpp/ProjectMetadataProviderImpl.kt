/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Usage
import org.gradle.api.file.FileCollection
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.awaitMetadataTarget
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle.Stage.AfterEvaluateBuildscript
import org.jetbrains.kotlin.gradle.plugin.mpp.MetadataDependencyResolution.ChooseVisibleSourceSets.MetadataProvider.ProjectMetadataProvider
import org.jetbrains.kotlin.gradle.targets.metadata.awaitMetadataCompilationsCreated
import org.jetbrains.kotlin.gradle.targets.metadata.locateOrRegisterGenerateProjectStructureMetadataTask
import org.jetbrains.kotlin.gradle.utils.setAttribute

private typealias SourceSetName = String

internal fun ProjectMetadataProvider(
    sourceSetMetadataOutputs: Map<SourceSetName, SourceSetMetadataOutputs>,
): ProjectMetadataProvider {
    return ProjectMetadataProviderImpl(sourceSetMetadataOutputs)
}

internal class SourceSetMetadataOutputs(
    val metadata: FileCollection?,
)

private class ProjectMetadataProviderImpl(
    private val sourceSetMetadataOutputs: Map<SourceSetName, SourceSetMetadataOutputs>,
) : ProjectMetadataProvider() {

    override fun getSourceSetCompiledMetadata(sourceSetName: String): FileCollection? {
        val metadataOutputs = sourceSetMetadataOutputs[sourceSetName] ?: return null
        return metadataOutputs.metadata
    }

}

internal suspend fun Project.collectSourceSetMetadataOutputs(): Map<SourceSetName, SourceSetMetadataOutputs> {
    /*
    Usually we can safely access the kotlin project extension inside a coroutine, as the Kotlin Gradle Plugin is
    the only entity that could launch coroutines (hence the extension being available).

    However, this code is crossing project boundaries here:
    There is _some_ Kotlin Gradle plugin that requests the 'ProjectData' being collected for
    *all* projects (breaking project isolation).

    Therefore, it might happen that the Kotlin Plugin was not even applied at this point, when this
    coroutine starts executing. We therefore await the wait for after the buildscript was evaluated to check
    if the multiplatformExtension is present.
     */
    AfterEvaluateBuildscript.await()
    val multiplatformExtension = multiplatformExtensionOrNull ?: return emptyMap()

    val sourceSetMetadata = multiplatformExtension.sourceSetsMetadataOutputs()

    return sourceSetMetadata.mapValues { (_, metadata) ->
        SourceSetMetadataOutputs(metadata = metadata)
    }.mapKeys { it.key.name }
}

internal val MetadataApiElementsSecondaryVariantsSetupAction = KotlinProjectSetupAction {
    project.launch {
        val multiplatformExtension = project.multiplatformExtension

        val metadataApiConfiguration =
            project.configurations.getByName(multiplatformExtension.awaitMetadataTarget().apiElementsConfigurationName)

        metadataApiConfiguration.addSecondaryOutgoingVariant(project)
    }
}

private fun Configuration.addSecondaryOutgoingVariant(project: Project) {

    val apiClassesVariant = outgoing.variants.maybeCreate("sourceSetsSecondaryVariant")

    apiClassesVariant.attributes.setAttribute(Usage.USAGE_ATTRIBUTE, project.usageByName(KotlinUsages.KOTLIN_LOCAL_METADATA))

    project.launch {
        val metadataCompilations = project.multiplatformExtension.kotlinMetadataCompilations()

        val generateProjectStructureMetadataTaskProvider = project.locateOrRegisterGenerateProjectStructureMetadataTask()
        apiClassesVariant.artifact(generateProjectStructureMetadataTaskProvider.flatMap { it.sourceSetMetadataOutputsFile }) { configureAction ->
            configureAction.classifier = "source-sets-metadata"
            configureAction.builtBy(generateProjectStructureMetadataTaskProvider)
            metadataCompilations.forEach { compilation ->
                configureAction.builtBy(compilation.output.classesDirs.buildDependencies)
            }
        }
    }


}

internal fun GenerateProjectStructureMetadata.addMetadataSourceSetsToOutput(project: Project) {
    val generateTask = this
    project.launch {
        val sourceSetOutputs =
            project.multiplatformExtension.kotlinMetadataCompilations()
                .map {
                    GenerateProjectStructureMetadata.SourceSetMetadataOutput(
                        sourceSetName = it.defaultSourceSet.name,
                        metadataOutput = project.provider { it.output.classesDirs.singleFile }
                    )
                }
        generateTask.sourceSetOutputs.set(sourceSetOutputs)
    }
}

internal suspend fun KotlinMultiplatformExtension.kotlinMetadataCompilations() = awaitMetadataTarget()
    .awaitMetadataCompilationsCreated()
    // TODO: KT-62332/Stop-Creating-legacy-metadata-compilation-with-name-main
    .filter { if (it is KotlinCommonCompilation) it.isKlibCompilation else true }


private suspend fun KotlinMultiplatformExtension.sourceSetsMetadataOutputs(): Map<KotlinSourceSet, FileCollection?> {
    return kotlinMetadataCompilations().associate { it.defaultSourceSet to it.output.classesDirs }
}