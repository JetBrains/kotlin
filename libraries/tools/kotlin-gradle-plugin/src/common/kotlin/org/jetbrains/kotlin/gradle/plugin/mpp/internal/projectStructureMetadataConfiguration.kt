/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.gradle.plugin.mpp.internal

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Attribute
import org.gradle.api.file.FileCollection
import org.jetbrains.kotlin.gradle.dsl.awaitMetadataTarget
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.configureMetadataDependenciesAttribute
import org.jetbrains.kotlin.gradle.plugin.mpp.resolvableMetadataConfiguration
import org.jetbrains.kotlin.gradle.plugin.sources.InternalKotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.sources.disambiguateName
import org.jetbrains.kotlin.gradle.targets.metadata.locateOrRegisterGenerateProjectStructureMetadataTask
import org.jetbrains.kotlin.gradle.utils.*

internal val psmAttribute = Attribute.of("org.jetbrains.kotlin.projectStructureMetadata", Boolean::class.javaObjectType)

/**
 * This Setup Action is required for creating psm configuration for non-KMP projects.
 * In fact, it only creates PSM consumable-configuration for the current project that can be consumed by another KMP project
 */
internal val ProjectStructureMetadataForJVMSetupAction = KotlinProjectSetupAction {
    maybeCreatePsmConsumableConfiguration(project)
}

internal val ProjectStructureMetadataForKMPSetupAction = KotlinProjectSetupAction {
    project.setupProjectStructureMetadataOutgoingArtifacts()
    project.dependencies.attributesSchema.attribute(psmAttribute)
    setupTransformActionFromJarToPsm(project)
}

/**
 * This method does two things:
 * 1) Create psm-consumable configuration for current projects, which contains psm file for this (output of psm-generation task)
 * and psm files for transitive projects as artifact.
 * 2) Adds psm file as artifact (output of psm-generation task) to `apiElements` configuration of metadata target.
 * @param project Current project
 */
internal fun Project.setupProjectStructureMetadataOutgoingArtifacts() {
    val project = this
    val psmConsumableConfiguration = maybeCreatePsmConsumableConfiguration(project)
    // KMP projects only have a project-structure-metadata and a metadata target,
    // but we still need to set up psm attribute for successful dependency resolution.
    val generateProjectStructureMetadata = project.locateOrRegisterGenerateProjectStructureMetadataTask()

    // We need it for wiring psm generation task's output with the outgoing artifact
    project.artifacts.add(
        psmConsumableConfiguration.name,
        generateProjectStructureMetadata.map { task -> task.resultFile }
    )

    project.launch {
        val metadataTarget = project.multiplatformExtension.awaitMetadataTarget()

        // Some transitive dependencies could contain links to submodules as well, thus we are wiring them here
        psmConsumableConfiguration.copyDependenciesLazy(
            project,
            project.configurations.getByName(metadataTarget.apiElementsConfigurationName)
        )
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

private fun maybeCreatePsmConsumableConfiguration(project: Project): Configuration {
    return project.configurations.maybeCreateConsumable(PSM_CONSUMABLE_CONFIGURATION_NAME) {
        configurePsmResolvableAttributes(project)
    }
}

private fun Configuration.configurePsmResolvableAttributes(project: Project) {
    attributes.setAttribute(psmAttribute, true)
    this.configureMetadataDependenciesAttribute(project)
}

private val InternalKotlinSourceSet.projectStructureMetadataConfigurationName: String
    get() = disambiguateName(lowerCamelCaseName(PSM_RESOLVABLE_CONFIGURATION_NAME))


private fun setupTransformActionFromJarToPsm(project: Project) {
    project.dependencies.artifactTypes.maybeCreate("jar").also { artifactType ->
        artifactType.attributes.setAttribute(psmAttribute, false)
    }

    project.dependencies.registerTransform(ProjectStructureMetadataTransformAction::class.java) { transform ->
        transform.from.setAttribute(psmAttribute, false)
        transform.to.setAttribute(psmAttribute, true)
    }
}