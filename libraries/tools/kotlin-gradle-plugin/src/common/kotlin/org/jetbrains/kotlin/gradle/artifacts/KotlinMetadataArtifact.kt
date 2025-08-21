/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.artifacts

import org.gradle.api.artifacts.type.ArtifactTypeDefinition.JAR_TYPE
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Usage
import org.gradle.api.file.FileCollection
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.categoryByName
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.plugin.usageByName
import org.jetbrains.kotlin.gradle.targets.metadata.awaitMetadataCompilationsCreated
import org.jetbrains.kotlin.gradle.targets.metadata.psmJarClassifier
import org.jetbrains.kotlin.gradle.targets.metadata.locateOrRegisterGenerateProjectStructureMetadataTask
import org.jetbrains.kotlin.gradle.targets.native.internal.includeCommonizedCInteropMetadata

internal val KotlinMetadataArtifact = KotlinTargetArtifact { target, apiElements, _ ->
    if (target !is KotlinMetadataTarget) return@KotlinTargetArtifact

    apiElements.attributes.attribute(Usage.USAGE_ATTRIBUTE, target.project.usageByName(KotlinUsages.KOTLIN_METADATA))
    apiElements.attributes.attribute(Category.CATEGORY_ATTRIBUTE, target.project.categoryByName(Category.LIBRARY))

    val metadataJarTask = target.createArtifactsTask { jar ->
        jar.description = "Assembles a jar archive containing the metadata for all Kotlin source sets."
        target.project.psmJarClassifier?.let {
            jar.archiveClassifier.set(it)
        }
    }

    /* Include 'KotlinProjectStructureMetadata' file */
    val generateMetadata = target.project.locateOrRegisterGenerateProjectStructureMetadataTask()
    metadataJarTask.configure { jar ->
        jar.from(generateMetadata.map { it.resultFile }) { spec ->
            spec.into("META-INF").rename { MULTIPLATFORM_PROJECT_METADATA_JSON_FILE_NAME }
        }
    }

    /* Include output of metadata compilations into metadata jar (including commonizer output if available */
    val hostSpecificSourceSets = getHostSpecificSourceSets(target.project)
    target.publishedMetadataCompilations().filter {
        /* Filter 'host specific' source sets (aka source sets that require a certain host to compile metadata) */
        it.defaultSourceSet !in hostSpecificSourceSets
    }.forEach { compilation ->
        metadataJarTask.configure { it.from(compilation.metadataPublishedArtifacts) { spec -> spec.into(compilation.metadataFragmentIdentifier) } }
        if (compilation is KotlinSharedNativeCompilation) {
            target.project.includeCommonizedCInteropMetadata(metadataJarTask, compilation)
        }
    }

    target.createPublishArtifact(metadataJarTask, JAR_TYPE, apiElements)
}

internal suspend fun KotlinMetadataTarget.publishedMetadataCompilations(): List<KotlinCompilation<*>> {
    return awaitMetadataCompilationsCreated().filter { compilation ->
        /* Filter legacy compilation */
        !(compilation is KotlinCommonCompilation && !compilation.isKlibCompilation)
    }
}

internal val KotlinCompilation<*>.metadataPublishedArtifacts: FileCollection
    get() = output.classesDirs

/**
 * Name of the fragment in the metadata jar/intermediate fragment in uklib is derived from the source set name
 */
internal val KotlinSourceSet.metadataFragmentIdentifier: String
    get() = name
internal val KotlinCompilation<*>.metadataFragmentIdentifier: String
    get() = defaultSourceSet.metadataFragmentIdentifier
