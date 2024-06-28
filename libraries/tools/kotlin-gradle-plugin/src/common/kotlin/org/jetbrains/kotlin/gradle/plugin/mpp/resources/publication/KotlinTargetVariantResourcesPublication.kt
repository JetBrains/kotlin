/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.resources.publication

import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.bundling.Zip
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.launch
import org.jetbrains.kotlin.gradle.plugin.mpp.AbstractKotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.DefaultKotlinUsageContext
import org.jetbrains.kotlin.gradle.plugin.mpp.internal
import org.jetbrains.kotlin.gradle.plugin.mpp.resources.KotlinTargetResourcesPublicationImpl
import org.jetbrains.kotlin.gradle.plugin.mpp.resources.KotlinTargetResourcesPublicationImpl.Companion.RESOURCES_CLASSIFIER
import org.jetbrains.kotlin.gradle.plugin.mpp.resources.KotlinTargetResourcesPublicationImpl.Companion.RESOURCES_ZIP_EXTENSION
import org.jetbrains.kotlin.gradle.plugin.mpp.resources.assembleHierarchicalResources
import org.jetbrains.kotlin.gradle.plugin.mpp.resources.resourcesPublicationExtension
import org.jetbrains.kotlin.gradle.tasks.registerTask

// Use KotlinMultiplatformExtension to make sure this usage context is only creatable in MPP
@Suppress("UnusedReceiverParameter")
internal fun AbstractKotlinTarget.setUpResourcesVariant(
    compilation: KotlinCompilation<*>,
): DefaultKotlinUsageContext? {
    if (project.multiplatformExtensionOrNull == null || !project.kotlinPropertiesProvider.mppResourcesPublication) return null

    var targetRegistersResourcesForPublication = false
    val resourcesVariant = DefaultKotlinUsageContext(
        compilation = compilation,
        dependencyConfigurationName = resourcesElementsConfigurationName,
        includeIntoProjectStructureMetadata = false,
        publishOnlyIf = {
            targetRegistersResourcesForPublication
        }
    )

    project.multiplatformExtension.resourcesPublicationExtension?.subscribeOnPublishResources(this) { resources ->
        targetRegistersResourcesForPublication = true
        val copyTask = compilation.assembleHierarchicalResources(
            targetName,
            resources,
        )
        val zippedResourcesDirectory = project.layout.buildDirectory.dir(
            "${KotlinTargetResourcesPublicationImpl.MULTIPLATFORM_RESOURCES_DIRECTORY}/zip-for-publication/${targetName}"
        )
        val zipResourcesForPublication = project.registerTask<Zip>(
            "${targetName}ZipMultiplatformResourcesForPublication"
        ) { copy ->
            copy.destinationDirectory.set(zippedResourcesDirectory)
            copy.duplicatesStrategy = DuplicatesStrategy.FAIL
            copy.archiveExtension.set(RESOURCES_ZIP_EXTENSION)
        }
        zipResourcesForPublication.configure {
            it.from(copyTask)
        }

        project.artifacts.add(
            compilation.target.internal.resourcesElementsConfigurationName,
            zipResourcesForPublication
        ) { artifact ->
            artifact.extension = RESOURCES_ZIP_EXTENSION
            artifact.classifier = RESOURCES_CLASSIFIER
            artifact.type = "zip"
        }
    }

    return resourcesVariant
}