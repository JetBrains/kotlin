/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.resources.publication

import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.bundling.Zip
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.launchInStage
import org.jetbrains.kotlin.gradle.plugin.mpp.AbstractKotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.DefaultKotlinUsageContext
import org.jetbrains.kotlin.gradle.plugin.mpp.archive.KotlinTargetWithKotlinArchiveSupport
import org.jetbrains.kotlin.gradle.plugin.mpp.archive.defaultKotlinUsageContextMaybeReplacedWithKar
import org.jetbrains.kotlin.gradle.plugin.mpp.archive.karAssembleTask
import org.jetbrains.kotlin.gradle.plugin.mpp.internal
import org.jetbrains.kotlin.gradle.plugin.mpp.resources.KotlinTargetResourcesPublicationImpl
import org.jetbrains.kotlin.gradle.plugin.mpp.resources.KotlinTargetResourcesPublicationImpl.Companion.RESOURCES_CLASSIFIER
import org.jetbrains.kotlin.gradle.plugin.mpp.resources.KotlinTargetResourcesPublicationImpl.Companion.RESOURCES_ZIP_EXTENSION
import org.jetbrains.kotlin.gradle.plugin.mpp.resources.assembleHierarchicalResources
import org.jetbrains.kotlin.gradle.plugin.mpp.resources.resourcesPublicationExtension
import org.jetbrains.kotlin.gradle.tasks.registerTask

// Use KotlinMultiplatformExtension to make sure this usage context is only creatable in MPP
internal fun AbstractKotlinTarget.setUpResourcesVariant(
    compilation: KotlinCompilation<*>,
): DefaultKotlinUsageContext? {
    if (project.multiplatformExtensionOrNull == null || !project.kotlinPropertiesProvider.mppResourcesPublication) return null

    var targetRegistersResourcesForPublication = false
    val resourcesVariant = project.defaultKotlinUsageContextMaybeReplacedWithKar(
        isStoredInKotlinArchive = if (this is KotlinTargetWithKotlinArchiveSupport) isStoredInKotlinArchive else null,
        compilation = compilation,
        mavenScope = null,
        dependencyConfigurationName = resourcesElementsConfigurationName,
        includeIntoProjectStructureMetadata = false,
        publishOnlyIf = {
            targetRegistersResourcesForPublication
        }
    )

    project.multiplatformExtension.resourcesPublicationExtension?.subscribeOnPublishResources(this) { resources ->
        targetRegistersResourcesForPublication = true
        val copyTaskOutputDirectory = compilation.assembleHierarchicalResources(
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
            it.from(copyTaskOutputDirectory)
        }

        project.artifacts.add(
            compilation.target.internal.resourcesElementsConfigurationName,
            zipResourcesForPublication
        ) { artifact ->
            artifact.extension = RESOURCES_ZIP_EXTENSION
            artifact.classifier = RESOURCES_CLASSIFIER
            artifact.type = "zip"
        }

        if (this is KotlinTargetWithKotlinArchiveSupport) {
            // delay is required to read isStoredInKotlinArchive
            project.launchInStage(KotlinPluginLifecycle.Stage.AfterFinaliseDsl) {
                if (isStoredInKotlinArchive.get()) {
                    val fileCollection = project.layout.files(copyTaskOutputDirectory)
                    project.karAssembleTask.configure { task ->
                        task.addResources(
                            platformNameInKotlinArchive,
                            fileCollection
                        )
                    }
                }
            }
        }
    }

    return resourcesVariant
}
