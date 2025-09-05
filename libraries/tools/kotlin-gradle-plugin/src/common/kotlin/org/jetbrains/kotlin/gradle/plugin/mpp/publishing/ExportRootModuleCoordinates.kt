/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.publishing

import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.jetbrains.kotlin.gradle.dsl.awaitMetadataTarget
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinProjectSetupCoroutine
import org.jetbrains.kotlin.gradle.plugin.internal.KotlinShareableDataAsSecondaryVariant
import org.jetbrains.kotlin.gradle.plugin.internal.KotlinSecondaryVariantsDataSharing
import org.jetbrains.kotlin.gradle.plugin.internal.compatAccessor
import org.jetbrains.kotlin.gradle.plugin.internal.kotlinSecondaryVariantsDataSharing
import org.jetbrains.kotlin.gradle.plugin.mpp.ModuleDependencyIdentifier
import org.jetbrains.kotlin.gradle.plugin.mpp.ModuleIds
import org.jetbrains.kotlin.gradle.plugin.mpp.resolvableMetadataConfiguration
import org.jetbrains.kotlin.gradle.plugin.sources.InternalKotlinSourceSet
import org.jetbrains.kotlin.gradle.utils.currentBuildId

/**
 * Exported KMP Coordinates to be consumed by other KMP projects
 */
internal data class KotlinProjectCoordinatesData(
    @get:Input
    val buildPath: String,

    @get:Input
    val projectPath: String,

    @get:Nested
    val moduleId: ModuleDependencyIdentifier,
) : KotlinShareableDataAsSecondaryVariant

private const val PROJECT_DATA_SHARING_KEY = "rootPublicationCoordinates"

internal val ExportRootModuleCoordinates = KotlinProjectSetupCoroutine {
    val metadataTarget = multiplatformExtension.awaitMetadataTarget()

    val projectDataSharingService = project.kotlinSecondaryVariantsDataSharing

    val metadataApiElements = configurations.getByName(metadataTarget.apiElementsConfigurationName)
    val coordinates = collectKotlinProjectCoordinates()
    projectDataSharingService.shareDataFromProvider(PROJECT_DATA_SHARING_KEY, metadataApiElements, provider { coordinates })
}

internal fun KotlinSecondaryVariantsDataSharing.consumeRootModuleCoordinates(sourceSet: InternalKotlinSourceSet) = consume(
    PROJECT_DATA_SHARING_KEY,
    sourceSet.resolvableMetadataConfiguration,
    KotlinProjectCoordinatesData::class.java
)

private suspend fun Project.collectKotlinProjectCoordinates(): KotlinProjectCoordinatesData {
    return KotlinProjectCoordinatesData(
        buildPath = project.currentBuildId().compatAccessor(project).buildPath,
        projectPath = project.path,
        moduleId = ModuleIds.idOfRootModuleSafe(this)
    )
}
