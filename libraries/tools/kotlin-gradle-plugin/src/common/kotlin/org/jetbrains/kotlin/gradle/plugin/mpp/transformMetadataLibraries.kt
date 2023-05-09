/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.jetbrains.kotlin.gradle.plugin.mpp.MetadataDependencyResolution.ChooseVisibleSourceSets.MetadataProvider.ArtifactMetadataProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.MetadataDependencyResolution.ChooseVisibleSourceSets.MetadataProvider.ProjectMetadataProvider
import java.io.File

/**
 * Returns a map from 'visibleSourceSetName' to the transformed metadata libraries.
 * The map is necessary to support [MetadataDependencyTransformation]'s shape, which
 * is used in import and therefore hard to change.
 *
 * This function will also support project to project dependencies and just returns the compiled output FileCollections to the metadata.
 */
internal fun Project.transformMetadataLibrariesForIde(
    resolution: MetadataDependencyResolution.ChooseVisibleSourceSets
): Map<String /* visibleSourceSetName */, Iterable<File>> {
    return when (val metadataProvider = resolution.metadataProvider) {
        is ProjectMetadataProvider -> resolution.visibleSourceSetNamesExcludingDependsOn.associateWith { visibleSourceSetName ->
            metadataProvider.getSourceSetCompiledMetadata(visibleSourceSetName) ?: emptyList()
        }

        is ArtifactMetadataProvider -> transformMetadataLibrariesForIde(
            kotlinTransformedMetadataLibraryDirectoryForIde, resolution, metadataProvider
        )
    }
}

/**
 * Will transform the [CompositeMetadataArtifact] extracting the visible source sets specified in the [resolution]
 * @param materializeFiles: If true, the klib files will actually be created and extracted
 *
 * In case the [resolution] points to a project dependency, then the output file collections will be returned.
 */
internal fun ObjectFactory.transformMetadataLibrariesForBuild(
    resolution: MetadataDependencyResolution.ChooseVisibleSourceSets, outputDirectory: File, materializeFiles: Boolean
): Iterable<File> {
    return when (resolution.metadataProvider) {
        is ProjectMetadataProvider -> fileCollection().from(
            resolution.visibleSourceSetNamesExcludingDependsOn.map { visibleSourceSetName ->
                resolution.metadataProvider.getSourceSetCompiledMetadata(visibleSourceSetName)
            }
        )

        is ArtifactMetadataProvider -> transformMetadataLibrariesForBuild(
            resolution, outputDirectory, materializeFiles, resolution.metadataProvider
        )
    }
}

/* Implementations for transforming the Composite Metadata Artifact */

private fun transformMetadataLibrariesForIde(
    baseOutputDirectory: File,
    resolution: MetadataDependencyResolution.ChooseVisibleSourceSets,
    compositeMetadataArtifact: CompositeMetadataArtifact
): Map<String /* visibleSourceSetName */, Iterable<File>> {
    return compositeMetadataArtifact.read { artifactContent ->
        resolution.visibleSourceSetNamesExcludingDependsOn.mapNotNull { visibleSourceSetName ->
            val sourceSetContent = artifactContent.findSourceSet(visibleSourceSetName) ?: return@mapNotNull null
            val sourceSetMetadataBinary = sourceSetContent.metadataBinary ?: return@mapNotNull null
            val metadataLibraryOutputFile = baseOutputDirectory.resolve(sourceSetMetadataBinary.relativeFile)
            metadataLibraryOutputFile.parentFile.mkdirs()
            if (!metadataLibraryOutputFile.exists()) {
                sourceSetMetadataBinary.copyTo(metadataLibraryOutputFile)
                if (!metadataLibraryOutputFile.exists()) return@mapNotNull null
            }

            visibleSourceSetName to listOf(metadataLibraryOutputFile)
        }.toMap()
    }
}

private fun transformMetadataLibrariesForBuild(
    resolution: MetadataDependencyResolution.ChooseVisibleSourceSets,
    outputDirectory: File,
    materializeFiles: Boolean,
    compositeMetadataArtifact: CompositeMetadataArtifact
): Set<File> {
    /*
    Careful handling of composite builds:
    Right now, composite multiplatform builds will compile against the composite artifact build by the producer build (not
    against compiled source sets directly).

    This means, that this function might be called to figure out task inputs at a time, where
    this composite artifact is not present on disk yet.
     */
    if (!materializeFiles && !compositeMetadataArtifact.exists()) return emptySet()

    return compositeMetadataArtifact.read { artifactContent ->
        resolution.visibleSourceSetNamesExcludingDependsOn.mapNotNull { visibleSourceSetName ->
            val sourceSetContent = artifactContent.findSourceSet(visibleSourceSetName) ?: return@mapNotNull null
            val metadataBinary = sourceSetContent.metadataBinary ?: return@mapNotNull null
            val metadataLibraryFile = outputDirectory.resolve(metadataBinary.relativeFile)
            if (materializeFiles) {
                metadataLibraryFile.parentFile?.mkdirs()
                metadataBinary.copyTo(metadataLibraryFile)
            }
            metadataLibraryFile
        }
    }.toSet()
}
