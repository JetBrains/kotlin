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

    val dependencyTransformationTaskOutputs = project.files(dependencyTransformationTask.map { it.outputLibraryFiles })
    return if (forIde) {
        /*
            For IDE Import the classpath will be assembled by three independent parts:
            1) C-Interop Metadata which will be downloaded Jar files that get transformed by the transformation task
            2) C-Interop Metadata directly provided by dependency projects (in the same build)
            3) C-Interop Metadata from 'associated compilations' / additionalVisible source sets
               (e.g. 'nativeTest' will be able to access the classpath from 'nativeMain')
         */
        dependencyTransformationTaskOutputs +
                createCInteropMetadataDependencyClasspathFromProjectDependenciesForIde(sourceSet) +
                createCInteropMetadataDependencyClasspathFromAssociatedCompilations(sourceSet, true)
    } else {
        /*
            For CLI execution the classpath will be assembled from two parts:
            1) C-Interop metadata from the transformation task which transforms
                Project Dependencies and External Module Dependencies (e.g. the ones that downloaded from maven repo)
            2) C-Interop Metadata from 'associated compilations' / additionalVisible source sets
               (e.g. 'nativeTest' will be able to access the classpath from 'nativeMain')
         */
        dependencyTransformationTaskOutputs +
                createCInteropMetadataDependencyClasspathFromAssociatedCompilations(sourceSet, false)
    }
}

private fun Project.createCInteropMetadataDependencyClasspathFromProjectDependenciesForIde(
    sourceSet: DefaultKotlinSourceSet
): FileCollection {
    return filesProvider {
        sourceSet.metadataTransformation
            .metadataDependencyResolutionsOrEmpty
            .filterIsInstance<ChooseVisibleSourceSets>()
            .mapNotNull { chooseVisibleSourceSets ->
                /* We only want to access resolutions that provide metadata from dependency projects */
                val projectMetadataProvider = when (chooseVisibleSourceSets.metadataProvider) {
                    is ProjectMetadataProvider -> chooseVisibleSourceSets.metadataProvider
                    is ArtifactMetadataProvider -> return@mapNotNull null
                }

                chooseVisibleSourceSets.visibleSourceSetProvidingCInterops?.let { visibleSourceSetName ->
                    projectMetadataProvider.getSourceSetCInteropMetadata(visibleSourceSetName, Ide)
                }
            }
    }
}

private fun Project.createCInteropMetadataDependencyClasspathFromAssociatedCompilations(
    sourceSet: DefaultKotlinSourceSet,
    forIde: Boolean
): FileCollection {
    return filesProvider files@{
        val commonizerTarget = sourceSet.sharedCommonizerTarget.getOrThrow() ?: return@files emptySet<File>()

        /*
        We will find the 'most suitable' / 'closest matching' source set
        (like 'nativeTest' -> 'nativeMain', 'appleTest' -> 'appleMain', ...).
        If no source set is found that matches the commonizer target explicitly, the next "bigger" source set shall be chosen
         */
        val (associatedSourceSet, _) = sourceSet.getAdditionalVisibleSourceSets()
            .filterIsInstance<DefaultKotlinSourceSet>()
            .mapNotNull { otherSourceSet -> otherSourceSet to (otherSourceSet.sharedCommonizerTarget.getOrThrow() ?: return@mapNotNull null) }
            .filter { (_, otherCommonizerTarget) -> otherCommonizerTarget.targets.containsAll(commonizerTarget.targets) }
            .minByOrNull { (_, otherCommonizerTarget) -> otherCommonizerTarget.targets.size } ?: return@files emptySet<File>()

        createCInteropMetadataDependencyClasspath(associatedSourceSet, forIde)
    }
}

/**
 * Names of all source sets that may potentially provide necessary cinterops for this resolution.
 * This will select 'the most bottom' source sets in [ChooseVisibleSourceSets.allVisibleSourceSetNames]
 */
internal val ChooseVisibleSourceSets.visibleSourceSetProvidingCInterops: String?
    get() {
        val dependsOnSourceSets = allVisibleSourceSetNames
            .flatMap { projectStructureMetadata.sourceSetsDependsOnRelation[it].orEmpty() }
            .toSet()

        val bottomSourceSets = allVisibleSourceSetNames.filter { it !in dependsOnSourceSets }.toSet()

        /* Select the source set participating in the least amount of variants (the most special one) */
        return bottomSourceSets.minByOrNull { sourceSetName ->
            projectStructureMetadata.sourceSetNamesByVariantName.count { (_, sourceSetNames) -> sourceSetName in sourceSetNames }
        }
    }
