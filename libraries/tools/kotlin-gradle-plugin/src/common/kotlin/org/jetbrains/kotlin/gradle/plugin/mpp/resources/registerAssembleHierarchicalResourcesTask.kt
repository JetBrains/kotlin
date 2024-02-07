/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.resources

import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.tasks.locateOrRegisterTask

internal suspend fun KotlinCompilation<*>.registerAssembleHierarchicalResourcesTask(
    targetNamePrefix: String,
    resources: KotlinTargetResourcesPublicationImpl.TargetResources,
): Provider<Directory> {
    KotlinPluginLifecycle.Stage.AfterFinaliseRefinesEdges.await()

    val resourceDirectoriesByLevel = splitResourceDirectoriesBySourceSetLevel(
        resources = resources,
        rootSourceSets = kotlinSourceSets.toList(),
    )
    val outputDirectory = project.layout.buildDirectory.dir(
        "${KotlinTargetResourcesPublicationImpl.MULTIPLATFORM_RESOURCES_DIRECTORY}/assemble-hierarchically/${targetNamePrefix}"
    )

    var isAlreadyConfigured = false
    val copyResourcesByLevel = project.locateOrRegisterTask<AssembleHierarchicalResourcesTask>(
        "${targetNamePrefix}CopyHierarchicalMultiplatformResources",
        invokeWhenRegistered = {
            configure { copy ->
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
        },
        configureTask = {
            if (isAlreadyConfigured) {
                error("FIXME: Support multiple TargetResources per target?")
            }
            isAlreadyConfigured = true
        }
    )

    return copyResourcesByLevel.flatMap { it.outputDirectory }
}

private fun splitResourceDirectoriesBySourceSetLevel(
    resources: KotlinTargetResourcesPublicationImpl.TargetResources,
    rootSourceSets: List<KotlinSourceSet>,
): List<List<KotlinTargetResourcesPublication.ResourceDescriptor>> {
    val resourceDirectoriesToProcess: MutableList<MutableList<KotlinTargetResourcesPublication.ResourceDescriptor>> = mutableListOf()
    val visitedSourceSets: MutableSet<KotlinSourceSet> = mutableSetOf()

    var sourceSetsAtThisLevel: MutableList<KotlinSourceSet> = rootSourceSets.toMutableList()

    while (sourceSetsAtThisLevel.isNotEmpty()) {
        resourceDirectoriesToProcess.add(mutableListOf())
        val sourceSetsAtNextLevel: MutableList<KotlinSourceSet> = mutableListOf()

        while (sourceSetsAtThisLevel.isNotEmpty()) {
            val sourceSet = sourceSetsAtThisLevel.pop()
            if (sourceSet in visitedSourceSets) {
                continue
            }
            visitedSourceSets.add(sourceSet)

            resourceDirectoriesToProcess.last().add(resources.resourcePathForSourceSet(sourceSet))

            // FIXME: dependsOn is a Set with non-deterministic ordering
            sourceSetsAtNextLevel.addAll(sourceSet.dependsOn)
        }
        sourceSetsAtThisLevel = sourceSetsAtNextLevel
    }

    return resourceDirectoriesToProcess.reversed()
}