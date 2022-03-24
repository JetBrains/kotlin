/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.jetbrains.kotlin.gradle.plugin.mpp.MetadataDependencyResolution.ChooseVisibleSourceSets
import org.jetbrains.kotlin.gradle.plugin.mpp.MetadataDependencyResolution.ChooseVisibleSourceSets.MetadataProvider.JarMetadataProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.MetadataDependencyResolution.ChooseVisibleSourceSets.MetadataProvider.ProjectMetadataProvider
import java.io.File

/**
 * @param outputDirectoryWhenMaterialised: Root output directory to put files if [materializeFilesIfNecessary] is set
 * and the artifact is indeed a [CompositeMetadataJar]
 *
 * @param materializeFilesIfNecessary: If the artifact is a [CompositeMetadataJar] then this flag will tell if
 * compiled source set metadata should be extracted from this jar file into the given [outputDirectoryWhenMaterialised]
 */
internal fun ChooseVisibleSourceSets.getAllCompiledSourceSetMetadata(
    project: Project,
    outputDirectoryWhenMaterialised: File,
    materializeFilesIfNecessary: Boolean
): ConfigurableFileCollection = project.files(
    visibleSourceSetNamesExcludingDependsOn.map { visibleSourceSetName ->
        metadataProvider.getSourceSetCompiledMetadata(
            project, visibleSourceSetName, outputDirectoryWhenMaterialised, materializeFilesIfNecessary
        )
    }
)

internal fun ChooseVisibleSourceSets.MetadataProvider.getSourceSetCompiledMetadata(
    project: Project,
    sourceSetName: String,
    outputDirectoryWhenMaterialised: File,
    materializeFilesIfNecessary: Boolean
): FileCollection = when (this) {
    is ProjectMetadataProvider -> getSourceSetCompiledMetadata(sourceSetName)
    is JarMetadataProvider -> project.files(
        getSourceSetCompiledMetadata(
            sourceSetName, outputDirectoryWhenMaterialised, materializeFilesIfNecessary
        )
    )
}
