/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.gradle.plugin.mpp.internal

import org.gradle.api.Project
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.attributes.Usage
import org.gradle.api.file.FileCollection
import org.jetbrains.kotlin.gradle.dsl.awaitMetadataTarget
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinProjectSetupAction
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.launch
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.plugin.mpp.kotlinMetadataCompilations
import org.jetbrains.kotlin.gradle.plugin.mpp.resolvableMetadataConfiguration
import org.jetbrains.kotlin.gradle.plugin.sources.InternalKotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.sources.internal
import org.jetbrains.kotlin.gradle.plugin.usageByName
import org.jetbrains.kotlin.gradle.targets.metadata.locateOrRegisterGenerateProjectStructureMetadataTask
import org.jetbrains.kotlin.gradle.utils.*

internal val ProjectStructureMetadataForKMPSetupAction = KotlinProjectSetupAction {
    project.setupProjectStructureMetadataOutgoingArtifacts()
    setupTransformActionFromJarToPsm(project)
}

/**
 * Add a secondary variant to the metadataApiElements configuration,
 * which contains psm file for this project (output of psm-generation task)
 */
internal fun Project.setupProjectStructureMetadataOutgoingArtifacts() {
    val project = this
    val generateProjectStructureMetadata = project.locateOrRegisterGenerateProjectStructureMetadataTask()

    project.launch {
        val metadataTarget = project.multiplatformExtension.awaitMetadataTarget()
        val apiElements = project.configurations.getByName(metadataTarget.apiElementsConfigurationName)

        apiElements.outgoing.variants.maybeCreate("kotlinProjectStructureMetadata").apply {
            setAttribute(Usage.USAGE_ATTRIBUTE, project.usageByName(KotlinUsages.KOTLIN_PSM_METADATA))
            registerArtifact(
                artifactProvider = generateProjectStructureMetadata.map { task -> task.resultFile },
                classifier = "psm-metadata",
                type = KotlinUsages.KOTLIN_PSM_METADATA,
            )
        }
    }
}

internal fun InternalKotlinSourceSet.projectStructureMetadataResolvedConfiguration(): LazyResolvedConfiguration {
    return LazyResolvedConfiguration(resolvableMetadataConfiguration) { attributes ->
        attributes.setAttribute(Usage.USAGE_ATTRIBUTE, project.usageByName(KotlinUsages.KOTLIN_PSM_METADATA))
        attributes.setAttribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, KotlinUsages.KOTLIN_PSM_METADATA)
    }
}

internal suspend fun Project.psmArtifactsForAllDependencies(): List<FileCollection> {
    if (!kotlinPropertiesProvider.kotlinKmpProjectIsolationEnabled) return emptyList()
    return multiplatformExtension.kotlinMetadataCompilations().map { compilation ->
        compilation.defaultSourceSet.internal.projectStructureMetadataResolvedConfiguration().files
    }
}

private fun setupTransformActionFromJarToPsm(project: Project) {
    project.dependencies.registerTransform(ProjectStructureMetadataTransformAction::class.java) { transform ->
        transform.from.apply {
            setAttribute(Usage.USAGE_ATTRIBUTE, project.usageByName(KotlinUsages.KOTLIN_METADATA))
            setAttribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, ArtifactTypeDefinition.JAR_TYPE)
        }
        transform.to.apply {
            setAttribute(Usage.USAGE_ATTRIBUTE, project.usageByName(KotlinUsages.KOTLIN_PSM_METADATA))
            setAttribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, KotlinUsages.KOTLIN_PSM_METADATA)
        }
    }
}