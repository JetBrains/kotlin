/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.internal

import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.jetbrains.kotlin.gradle.plugin.mpp.MetadataDependencyResolution.ChooseVisibleSourceSets
import org.jetbrains.kotlin.gradle.plugin.mpp.MetadataDependencyResolution.ChooseVisibleSourceSets.MetadataProvider.ArtifactMetadataProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.MetadataDependencyResolution.ChooseVisibleSourceSets.MetadataProvider.ProjectMetadataProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.MetadataDependencyResolution.ChooseVisibleSourceSets.MetadataProvider.ProjectMetadataProvider.MetadataConsumer.Cli
import org.jetbrains.kotlin.gradle.plugin.mpp.MetadataDependencyResolution.ChooseVisibleSourceSets.MetadataProvider.ProjectMetadataProvider.MetadataConsumer.Ide
import org.jetbrains.kotlin.gradle.plugin.mpp.metadataDependencyResolutionsOrEmpty
import org.jetbrains.kotlin.gradle.plugin.sources.DefaultKotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.sources.metadataTransformation
import org.jetbrains.kotlin.gradle.utils.filesProvider
import java.io.File

internal fun Project.createCInteropMetadataDependencyClasspath(sourceSet: DefaultKotlinSourceSet): FileCollection {
    return createCInteropMetadataDependencyClasspath(sourceSet, forIde = false)
}

internal fun Project.createCInteropMetadataDependencyClasspathForIde(sourceSet: DefaultKotlinSourceSet): FileCollection {
    return createCInteropMetadataDependencyClasspath(sourceSet, forIde = true)
}

/**
 * @param forIde: A different task for dependency transformation will be used. This task will not use the regular 'build' directory
 * as transformation output to ensure IDE still being able to resolve the dependencies even when the project is cleaned.
 */
internal fun Project.createCInteropMetadataDependencyClasspath(sourceSet: DefaultKotlinSourceSet, forIde: Boolean): FileCollection {
    val dependencyTransformationTask = if (forIde) locateOrRegisterCInteropMetadataDependencyTransformationTaskForIde(sourceSet)
    else locateOrRegisterCInteropMetadataDependencyTransformationTask(sourceSet)
    if (dependencyTransformationTask == null) return project.files()

    /*
    The classpath will be assembled by three independent parts
    1) C-Interop Metadata which will be downloaded Jar files that get transformed by the transformation task
    2) C-Interop Metadata directly provided by dependency projects (in the same build)
    3) C-Interop Metadata from 'associated compilations' / additionalVisible source sets
       (e.g. 'nativeTest' will be able to access the classpath from 'nativeMain')
     */
    return project.files(dependencyTransformationTask.map { it.outputLibraryFiles }) +
            createCInteropMetadataDependencyClasspathFromProjectDependencies(sourceSet, forIde) +
            createCInteropMetadataDependencyClasspathFromAssociatedCompilations(sourceSet, forIde)
}

private fun Project.createCInteropMetadataDependencyClasspathFromProjectDependencies(
    sourceSet: DefaultKotlinSourceSet,
    forIde: Boolean
): FileCollection {
    return filesProvider {
        sourceSet.metadataTransformation
            .metadataDependencyResolutionsOrEmpty
            .filterIsInstance<ChooseVisibleSourceSets>()
            .flatMap { chooseVisibleSourceSets ->
                /* We only want to access resolutions that provide metadata from dependency projects */
                val projectMetadataProvider = when (chooseVisibleSourceSets.metadataProvider) {
                    is ProjectMetadataProvider -> chooseVisibleSourceSets.metadataProvider
                    is ArtifactMetadataProvider -> return@flatMap emptyList()
                }

                chooseVisibleSourceSets.visibleSourceSetsProvidingCInterops.mapNotNull { visibleSourceSetName ->
                    projectMetadataProvider.getSourceSetCInteropMetadata(visibleSourceSetName, if (forIde) Ide else Cli)
                }
            }
    }
}

private fun Project.createCInteropMetadataDependencyClasspathFromAssociatedCompilations(
    sourceSet: DefaultKotlinSourceSet,
    forIde: Boolean
): FileCollection {
    return filesProvider files@{
        val commonizerTarget = getSharedCommonizerTarget(sourceSet) ?: return@files emptySet<File>()

        /*
        We will find the 'most suitable' / 'closest matching' source set
        (like 'nativeTest' -> 'nativeMain', 'appleTest' -> 'appleMain', ...).
        If no source set is found that matches the commonizer target explicitly, the next "bigger" source set shall be chosen
         */
        val (associatedSourceSet, _) = sourceSet.getAdditionalVisibleSourceSets()
            .filterIsInstance<DefaultKotlinSourceSet>()
            .mapNotNull { otherSourceSet -> otherSourceSet to (getSharedCommonizerTarget(otherSourceSet) ?: return@mapNotNull null) }
            .filter { (_, otherCommonizerTarget) -> otherCommonizerTarget.targets.containsAll(commonizerTarget.targets) }
            .minByOrNull { (_, otherCommonizerTarget) -> otherCommonizerTarget.targets.size } ?: return@files emptySet<File>()

        createCInteropMetadataDependencyClasspath(associatedSourceSet, forIde)
    }
}

/**
 * Names of all source sets that may potentially provide necessary cinterops for this resolution.
 * This will select 'the most bottom' source sets in [ChooseVisibleSourceSets.allVisibleSourceSetNames]
 */
internal val ChooseVisibleSourceSets.visibleSourceSetsProvidingCInterops: Set<String>
    get() {
        val dependsOnSourceSets = allVisibleSourceSetNames
            .flatMap { projectStructureMetadata.sourceSetsDependsOnRelation[it].orEmpty() }
            .toSet()

        return allVisibleSourceSetNames.filter { it !in dependsOnSourceSets }.toSet()
    }
