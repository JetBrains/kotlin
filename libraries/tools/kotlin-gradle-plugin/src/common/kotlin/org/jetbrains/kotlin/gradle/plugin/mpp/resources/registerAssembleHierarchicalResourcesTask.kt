/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.resources

import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.reportDiagnostic
import org.jetbrains.kotlin.gradle.tasks.locateTask
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.tooling.core.withClosureGroupingByDistance

internal suspend fun KotlinCompilation<*>.registerAssembleHierarchicalResourcesTask(
    targetNamePrefix: String,
    resources: KotlinTargetResourcesPublication.TargetResources,
): Provider<Directory> = registerAssembleHierarchicalResourcesTaskProvider(
    targetNamePrefix,
    resources,
).flatMap { it.outputDirectory }

internal suspend fun KotlinCompilation<*>.registerAssembleHierarchicalResourcesTaskProvider(
    targetNamePrefix: String,
    resources: KotlinTargetResourcesPublication.TargetResources,
): TaskProvider<AssembleHierarchicalResourcesTask> {
    val taskName = "${targetNamePrefix}CopyHierarchicalMultiplatformResources"
    val existingTask = project.tasks.locateTask<AssembleHierarchicalResourcesTask>(taskName)
    if (existingTask != null) {
        project.reportDiagnostic(KotlinToolingDiagnostics.ResourcePublishedMoreThanOncePerTarget(targetNamePrefix))
        return existingTask
    }

    KotlinPluginLifecycle.Stage.AfterFinaliseRefinesEdges.await()
    val resourceDirectoriesByLevel = splitResourceDirectoriesBySourceSetLevel(
        resources = resources,
        rootSourceSets = kotlinSourceSets.sortedBy { it.name },
    )
    val outputDirectory = project.layout.buildDirectory.dir(
        "${KotlinTargetResourcesPublicationImpl.MULTIPLATFORM_RESOURCES_DIRECTORY}/assemble-hierarchically/${targetNamePrefix}"
    )

    return project.registerTask<AssembleHierarchicalResourcesTask>(taskName) { copy ->
        resourceDirectoriesByLevel.forEach { level ->
            copy.resourceDirectoriesByLevel.add(
                level.map {
                    AssembleHierarchicalResourcesTask.Resource(
                        it.absolutePath,
                        it.includes,
                        it.excludes,
                    )
                }
            )
        }
        copy.relativeResourcePlacement.set(resources.relativeResourcePlacement)
        copy.outputDirectory.set(outputDirectory)
    }
}

private fun splitResourceDirectoriesBySourceSetLevel(
    resources: KotlinTargetResourcesPublication.TargetResources,
    rootSourceSets: List<KotlinSourceSet>,
): List<List<KotlinTargetResourcesPublication.ResourceRoot>> {
    return rootSourceSets.withClosureGroupingByDistance { it.dependsOn }.map { sourceSets ->
        sourceSets.map { sourceSet ->
            resources.resourcePathForSourceSet(sourceSet)
        }
    }.reversed()
}