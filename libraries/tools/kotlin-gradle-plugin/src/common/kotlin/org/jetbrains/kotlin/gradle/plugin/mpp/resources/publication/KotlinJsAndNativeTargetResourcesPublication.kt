/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.resources.publication

import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.bundling.Zip
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.launch
import org.jetbrains.kotlin.gradle.plugin.mpp.AbstractKotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.DefaultKotlinUsageContext
import org.jetbrains.kotlin.gradle.plugin.mpp.resources.KotlinTargetResourcesPublication
import org.jetbrains.kotlin.gradle.plugin.mpp.resources.registerAssembleHierarchicalResourcesTask
import org.jetbrains.kotlin.gradle.plugin.mpp.resources.resourcesPublicationExtension
import org.jetbrains.kotlin.gradle.tasks.registerTask

internal fun AbstractKotlinTarget.setUpResourcesVariant(compilation: KotlinCompilation<*>): DefaultKotlinUsageContext {
    var targetRegistersResourcesForPublication = false
    val resourcesVariant = DefaultKotlinUsageContext(
        compilation = compilation,
        dependencyConfigurationName = resourcesElementsConfigurationName,
        // FIXME: ???
        includeIntoProjectStructureMetadata = false,
        publishOnlyIf = {
            targetRegistersResourcesForPublication
        }
    )

    project.multiplatformExtension.resourcesPublicationExtension.subscribeOnPublishResources(this) { resources ->
        // FIXME: Is this logic single-threaded?
        targetRegistersResourcesForPublication = true
        project.launch {
            val copyTask = compilation.registerAssembleHierarchicalResourcesTask(
                targetName,
                resources,
            )
            val zippedResourcesDirectory = project.layout.buildDirectory.dir(
                "${KotlinTargetResourcesPublication.MULTIPLATFORM_RESOURCES_DIRECTORY}/zip-for-publication/${targetName}"
            )
            val zipResourcesForPublication = project.registerTask<Zip>(
                "${targetName}ZipMultiplatformResourcesForPublication"
            ) { copy ->
                copy.destinationDirectory.set(zippedResourcesDirectory)
                copy.duplicatesStrategy = DuplicatesStrategy.FAIL
            }
            zipResourcesForPublication.configure {
                it.from(copyTask)
            }

            val archiveName = "kotlin_resources.zip"
            project.artifacts.add(
                compilation.target.resourcesElementsConfigurationName,
                zipResourcesForPublication
            ) { artifact ->
                artifact.extension = archiveName
                artifact.type = archiveName
                artifact.classifier = null
            }
        }
    }

    return resourcesVariant
}