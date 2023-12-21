/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.artifacts

import org.gradle.api.artifacts.type.ArtifactTypeDefinition.JAR_TYPE
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Usage
import org.jetbrains.kotlin.gradle.plugin.categoryByName
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.plugin.usageByName
import org.jetbrains.kotlin.gradle.targets.metadata.createGenerateProjectStructureMetadataTask
import org.jetbrains.kotlin.gradle.targets.metadata.isCompatibilityMetadataVariantEnabled
import org.jetbrains.kotlin.gradle.targets.metadata.isKotlinGranularMetadataEnabled
import org.jetbrains.kotlin.gradle.targets.native.internal.includeCommonizedCInteropMetadata
import org.jetbrains.kotlin.gradle.utils.setAttribute

internal val KotlinMetadataArtifact = KotlinTargetArtifact { target, apiElements, _ ->
    if (target !is KotlinMetadataTarget || !target.project.isKotlinGranularMetadataEnabled) return@KotlinTargetArtifact

    apiElements.attributes.setAttribute(Usage.USAGE_ATTRIBUTE, target.project.usageByName(KotlinUsages.KOTLIN_METADATA))
    apiElements.attributes.setAttribute(Category.CATEGORY_ATTRIBUTE, target.project.categoryByName(Category.LIBRARY))

    val metadataJarTask = target.createArtifactsTask { jar ->
        jar.description = "Assembles a jar archive containing the metadata for all Kotlin source sets."
        if (target.project.isCompatibilityMetadataVariantEnabled) {
            jar.archiveClassifier.set("all")
        }
    }

    /* Include 'KotlinProjectStructureMetadata' file */
    val generateMetadata = target.project.createGenerateProjectStructureMetadataTask()
    metadataJarTask.configure { jar ->
        jar.from(generateMetadata.map { it.resultFile }) { spec ->
            spec.into("META-INF").rename { MULTIPLATFORM_PROJECT_METADATA_JSON_FILE_NAME }
        }
    }

    /* Include output of metadata compilations into metadata jar (including commonizer output if available */
    val hostSpecificSourceSets = getHostSpecificSourceSets(target.project)
    target.compilations.all { compilation ->
        /* Filter legacy compilation */
        if (compilation is KotlinCommonCompilation && !compilation.isKlibCompilation) return@all
        /* Filter 'host specific' source sets (aka source sets that require a certain host to compile metadata) */
        if (compilation.defaultSourceSet in hostSpecificSourceSets) return@all

        metadataJarTask.configure { it.from(compilation.output.allOutputs) { spec -> spec.into(compilation.defaultSourceSet.name) } }
        if (compilation is KotlinSharedNativeCompilation) {
            target.project.includeCommonizedCInteropMetadata(metadataJarTask, compilation)
        }
    }

    target.createPublishArtifact(metadataJarTask, JAR_TYPE, apiElements)
}
