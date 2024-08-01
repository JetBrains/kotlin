/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.gradle.plugin.mpp.internal

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Usage
import org.gradle.api.file.FileCollection
import org.jetbrains.kotlin.gradle.dsl.awaitMetadataTarget
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinProjectSetupAction
import org.jetbrains.kotlin.gradle.plugin.PSM_RESOLVABLE_CONFIGURATION_NAME
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.launch
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.plugin.mpp.configureMetadataDependenciesAttribute
import org.jetbrains.kotlin.gradle.plugin.mpp.resolvableMetadataConfiguration
import org.jetbrains.kotlin.gradle.plugin.sources.InternalKotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.sources.disambiguateName
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

        apiElements.outgoing.variants.maybeCreate("projectStructureMetadata").apply {
            artifact(generateProjectStructureMetadata.map { task -> task.resultFile }) {
                it.classifier = "psm-metadata"
            }
            setAttribute(Usage.USAGE_ATTRIBUTE, project.usageByName(KotlinUsages.KOTLIN_PSM_METADATA))
        }
    }
}

internal val InternalKotlinSourceSet.projectStructureMetadataResolvableConfiguration: Configuration? by extrasStoredProperty {
    if (project.kotlinPropertiesProvider.kotlinKmpProjectIsolationEnabled) {
        project.configurations.maybeCreateResolvable(projectStructureMetadataConfigurationName) {
            copyDependenciesLazy(project, resolvableMetadataConfiguration)
            configurePsmResolvableAttributes(project)
        }
    } else {
        null
    }
}

internal fun resolvableMetadataConfigurationForEachSourSet(project: Project): List<FileCollection> {
    return project.multiplatformExtension.sourceSets.mapNotNull { sourceSet ->
        if (sourceSet is InternalKotlinSourceSet) {
            sourceSet.projectStructureMetadataResolvableConfiguration?.lenientArtifactsView?.artifactFiles
        } else null
    }
}

private fun Configuration.configurePsmResolvableAttributes(project: Project) {
    this.configureMetadataDependenciesAttribute(project)
    setAttribute(Usage.USAGE_ATTRIBUTE, project.usageByName(KotlinUsages.KOTLIN_PSM_METADATA))
}

private val InternalKotlinSourceSet.projectStructureMetadataConfigurationName: String
    get() = disambiguateName(lowerCamelCaseName(PSM_RESOLVABLE_CONFIGURATION_NAME))


private fun setupTransformActionFromJarToPsm(project: Project) {
    project.dependencies.registerTransform(ProjectStructureMetadataTransformAction::class.java) { transform ->
        transform.from.setAttribute(Usage.USAGE_ATTRIBUTE, project.usageByName(KotlinUsages.KOTLIN_METADATA))
        transform.to.setAttribute(Usage.USAGE_ATTRIBUTE, project.usageByName(KotlinUsages.KOTLIN_PSM_METADATA))
    }
}