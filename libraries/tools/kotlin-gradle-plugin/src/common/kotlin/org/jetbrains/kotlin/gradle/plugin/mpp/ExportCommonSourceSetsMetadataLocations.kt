/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.jetbrains.kotlin.gradle.dsl.awaitMetadataTarget
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinProjectSetupCoroutine
import org.jetbrains.kotlin.gradle.plugin.internal.KotlinProjectSharedDataProvider
import org.jetbrains.kotlin.gradle.plugin.internal.KotlinSecondaryVariantsDataSharing
import org.jetbrains.kotlin.gradle.plugin.internal.KotlinShareableDataAsSecondaryVariant
import org.jetbrains.kotlin.gradle.plugin.internal.kotlinSecondaryVariantsDataSharing
import java.io.File

private const val SOURCE_SETS_METADATA_LOCATIONS_KEY = "commonSourceSetsMetadataLocations"

internal val ExportCommonSourceSetsMetadataLocations = KotlinProjectSetupCoroutine {
    val metadataCompilations = project.multiplatformExtension.kotlinMetadataCompilations()

    val metadataTarget = project.multiplatformExtension.awaitMetadataTarget()
    val metadataApiElements = project.configurations.getByName(metadataTarget.apiElementsConfigurationName)

    val sharingService = project.kotlinSecondaryVariantsDataSharing

    val locationsProvider = project.provider {
        val locationsBySourceSetName = metadataCompilations.associate {
            it.name to it.output.classesDirs.files.single()
        }
        SourceSetMetadataLocations(locationsBySourceSetName)
    }

    val taskDependencies = project.multiplatformExtension.kotlinMetadataCompilations()
        .map { compilation -> compilation.output.classesDirs.buildDependencies }
    sharingService.shareDataFromProvider(
        SOURCE_SETS_METADATA_LOCATIONS_KEY,
        metadataApiElements,
        locationsProvider,
        taskDependencies
    )
}

internal class SourceSetMetadataLocations(
    @get:Internal
    val locationBySourceSetName: Map<String, File>
) : KotlinShareableDataAsSecondaryVariant {
    @get:InputFiles
    val locations: Collection<File> get() = locationBySourceSetName.values
}



internal fun KotlinSecondaryVariantsDataSharing.consumeCommonSourceSetMetadataLocations(
    from: Configuration
): KotlinProjectSharedDataProvider<SourceSetMetadataLocations> =
    consume(SOURCE_SETS_METADATA_LOCATIONS_KEY, from, SourceSetMetadataLocations::class.java)