/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.gradle.plugin.mpp.internal

import org.gradle.api.Project
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.attributes.Usage
import org.gradle.api.file.FileCollection
import org.jetbrains.kotlin.gradle.dsl.awaitMetadataTarget
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinProjectSetupAction
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.launch
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.plugin.mpp.resolvableMetadataConfiguration
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.consumption.uklibViewAttribute
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.consumption.uklibViewAttributeMetadataCompilationOutputs
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.publication.KmpPublicationStrategy
import org.jetbrains.kotlin.gradle.plugin.sources.InternalKotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.sources.internal
import org.jetbrains.kotlin.gradle.plugin.sources.isSharedSourceSet
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
            attributes.attribute(Usage.USAGE_ATTRIBUTE, project.usageByName(KotlinUsages.KOTLIN_PSM_METADATA))
            registerArtifact(
                artifactProvider = generateProjectStructureMetadata.map { task -> task.resultFile },
                classifier = "psm-metadata",
                type = KotlinUsages.KOTLIN_PSM_METADATA,
            )
        }
    }
}

internal fun InternalKotlinSourceSet.projectStructureMetadataResolvedConfiguration(): LazyResolvedConfigurationWithArtifacts {
    return LazyResolvedConfigurationWithArtifacts(resolvableMetadataConfiguration) { attributes ->
        attributes.attribute(Usage.USAGE_ATTRIBUTE, project.usageByName(KotlinUsages.KOTLIN_PSM_METADATA))
        attributes.attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, KotlinUsages.KOTLIN_PSM_METADATA)
    }
}

internal suspend fun Project.interprojectUklibManifestView(): FileCollection? {
    when (project.kotlinPropertiesProvider.kmpPublicationStrategy) {
        KmpPublicationStrategy.StandardKMPPublication -> return null
        KmpPublicationStrategy.UklibPublicationInASingleComponentWithKMPPublication -> {}
    }
    val files = objects.fileCollection()
    project.multiplatformExtension.awaitSourceSets().forEach {
        if (!it.internal.isSharedSourceSet()) return@forEach
        files.from(it.internal.interprojectUklibManifestView())
    }
    return files
}

internal fun InternalKotlinSourceSet.interprojectUklibManifestView(): FileCollection {
    return resolvableMetadataConfiguration.incoming.artifactView { view ->
        view.componentFilter { it is ProjectComponentIdentifier }
        view.isLenient = true
    }.files
}

internal fun InternalKotlinSourceSet.interprojectUklibMetadataCompilationOutputView(): FileCollection {
    return resolvableMetadataConfiguration.incoming.artifactView { view ->
        view.componentFilter { it is ProjectComponentIdentifier }
        view.isLenient = true
        view.attributes {
            it.attribute(uklibViewAttribute, uklibViewAttributeMetadataCompilationOutputs)
        }
    }.files
}

internal suspend fun Project.psmArtifactsForAllDependenciesFromSharedSourceSets(): List<FileCollection> {
    if (!kotlinPropertiesProvider.kotlinKmpProjectIsolationEnabled) return emptyList()
    return multiplatformExtension.sourceSets.mapNotNull { sourceSet ->
        if (!sourceSet.internal.isSharedSourceSet()) return@mapNotNull null
        sourceSet.internal.projectStructureMetadataResolvedConfiguration().files
    }
}

private fun setupTransformActionFromJarToPsm(project: Project) {
    project.dependencies.registerTransformForArtifactType(
        ProjectStructureMetadataTransformAction::class.java,
        fromArtifactType = ArtifactTypeDefinition.JAR_TYPE,
        toArtifactType = KotlinUsages.KOTLIN_PSM_METADATA
    ) { transform ->
        transform.from.apply {
            attributes.attribute(Usage.USAGE_ATTRIBUTE, project.usageByName(KotlinUsages.KOTLIN_METADATA))
        }
        transform.to.apply {
            attributes.attribute(Usage.USAGE_ATTRIBUTE, project.usageByName(KotlinUsages.KOTLIN_PSM_METADATA))
        }
    }
}