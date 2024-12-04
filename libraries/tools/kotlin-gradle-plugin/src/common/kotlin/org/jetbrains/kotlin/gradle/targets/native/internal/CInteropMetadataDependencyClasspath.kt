/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.internal

import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.jetbrains.kotlin.gradle.plugin.mpp.MetadataDependencyResolution.ChooseVisibleSourceSets
import org.jetbrains.kotlin.gradle.plugin.sources.DefaultKotlinSourceSet
import org.jetbrains.kotlin.gradle.utils.filesProvider
import org.jetbrains.kotlin.gradle.utils.future
import java.io.File

internal suspend fun Project.createCInteropMetadataDependencyClasspath(sourceSet: DefaultKotlinSourceSet): FileCollection {
    return createCInteropMetadataDependencyClasspath(sourceSet, forIde = false)
}

internal suspend fun Project.createCInteropMetadataDependencyClasspathForIde(sourceSet: DefaultKotlinSourceSet): FileCollection {
    return createCInteropMetadataDependencyClasspath(sourceSet, forIde = true)
}

/**
 * @param forIde: A different task for dependency transformation will be used. This task will not use the regular 'build' directory
 * as transformation output to ensure IDE still being able to resolve the dependencies even when the project is cleaned.
 */
internal suspend fun Project.createCInteropMetadataDependencyClasspath(sourceSet: DefaultKotlinSourceSet, forIde: Boolean): FileCollection {
    val dependencyTransformationTask = if (forIde) locateOrRegisterCInteropMetadataDependencyTransformationTaskForIde(sourceSet)
    else locateOrRegisterCInteropMetadataDependencyTransformationTask(sourceSet)
    if (dependencyTransformationTask == null) return project.files()

    val dependencyTransformationTaskOutputs = project.files(dependencyTransformationTask.map { it.outputLibraryFiles })

    return dependencyTransformationTaskOutputs +
            createCInteropMetadataDependencyClasspathFromAssociatedCompilations(sourceSet, forIde) +
            createCommonizedCInteropDependencyConfigurationView(sourceSet)
}

private fun Project.createCInteropMetadataDependencyClasspathFromAssociatedCompilations(
    sourceSet: DefaultKotlinSourceSet,
    forIde: Boolean,
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
            .mapNotNull { other -> other to (other.sharedCommonizerTarget.getOrThrow() ?: return@mapNotNull null) }
            .filter { (_, otherCommonizerTarget) -> otherCommonizerTarget.targets.containsAll(commonizerTarget.targets) }
            .minByOrNull { (_, otherCommonizerTarget) -> otherCommonizerTarget.targets.size } ?: return@files emptySet<File>()

        project.future { createCInteropMetadataDependencyClasspath(associatedSourceSet, forIde) }.getOrThrow()
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
