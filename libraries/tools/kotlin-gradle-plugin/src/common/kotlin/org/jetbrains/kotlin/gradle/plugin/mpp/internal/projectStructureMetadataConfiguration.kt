/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.internal

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Usage
import org.gradle.api.file.FileCollection
import org.jetbrains.kotlin.gradle.dsl.awaitMetadataTarget
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.plugin.mpp.resolvableMetadataConfiguration
import org.jetbrains.kotlin.gradle.plugin.sources.InternalKotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.sources.disambiguateName
import org.jetbrains.kotlin.gradle.targets.metadata.locateOrRegisterGenerateProjectStructureMetadataTask
import org.jetbrains.kotlin.gradle.utils.*

internal val psmAttribute = Attribute.of("org.jetbrains.kotlin.psmFile", Boolean::class.javaObjectType)

internal val projectStructureMetadataOutgoingArtifactsSetupAction = KotlinProjectSetupAction {
    project.setupProjectStructureMetadataOutgoingArtifacts()
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

internal val InternalKotlinSourceSet.projectStructureMetadataResolvableConfiguration: Configuration by extrasStoredProperty {
    project.configurations.maybeCreateResolvable(projectStructureMetadataConfigurationName) {
        copyDependenciesLazy(project, resolvableMetadataConfiguration)
        configurePsmDependenciesAttributes(project)
    }
}

internal fun resolvableMetadataConfigurationForEachSourSet(project: Project): List<FileCollection> {
    return project.multiplatformExtension.sourceSets.mapNotNull { sourceSet ->
        if (sourceSet is InternalKotlinSourceSet) {
            LazyResolvedConfiguration(sourceSet.projectStructureMetadataResolvableConfiguration).files
        } else null
    }
}

private fun maybeCreatePsmConsumableConfiguration(project: Project): Configuration {
    return project.configurations.maybeCreateConsumable(PSM_CONSUMABLE_CONFIGURATION_NAME) {
        configurePsmDependenciesAttributes(project)
    }
}

private fun Configuration.configurePsmDependenciesAttributes(project: Project) {
    attributes.setAttribute(psmAttribute, true)
    attributes.setAttribute(Usage.USAGE_ATTRIBUTE, project.usageByName(KotlinUsages.KOTLIN_PSM))
    attributes.setAttribute(Category.CATEGORY_ATTRIBUTE, project.categoryByName(Category.LIBRARY))
}

private val InternalKotlinSourceSet.projectStructureMetadataConfigurationName: String
    get() = disambiguateName(lowerCamelCaseName(PSM_RESOLVABLE_CONFIGURATION_NAME))