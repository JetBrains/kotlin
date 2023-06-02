/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.ide.dependencyResolvers

import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinDependency
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinSourceCoordinates
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinSourceDependency
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.ide.IdeDependencyResolver
import org.jetbrains.kotlin.gradle.plugin.ide.IdeaKotlinProjectCoordinates
import org.jetbrains.kotlin.gradle.plugin.mpp.MetadataDependencyResolution
import org.jetbrains.kotlin.gradle.plugin.mpp.projectDependency
import org.jetbrains.kotlin.gradle.plugin.sources.DefaultKotlinSourceSet

internal object IdeVisibleMultiplatformSourceDependencyResolver : IdeDependencyResolver {
    override fun resolve(sourceSet: KotlinSourceSet): Set<IdeaKotlinDependency> {
        if (sourceSet !is DefaultKotlinSourceSet) return emptySet()
        return sourceSet.resolveMetadata<MetadataDependencyResolution.ChooseVisibleSourceSets>()
            /*
            Check if resolved dependency is pointing to the project that contains the Source Set
            Such 'loop' or 'self' dependencies can happen when e.g.
            1) 'project A' depends on 'project B'
            2) 'project B / test' depends on project A

            In this case project B tests will get a project dependency pointing to project B's main variant
            */
            .filter { resolution -> resolution.projectDependency(sourceSet.project) != sourceSet.project }
            .flatMap { resolution -> resolveSourceDependencies(resolution) }
            .toSet()
    }

    private fun resolveSourceDependencies(
        resolution: MetadataDependencyResolution.ChooseVisibleSourceSets,
    ): Iterable<IdeaKotlinDependency> {
        val projectComponentIdentifier = resolution.dependency.id as? ProjectComponentIdentifier ?: return emptyList()
        return resolution.allVisibleSourceSetNames.map { visibleSourceSetName ->
            IdeaKotlinSourceDependency(
                type = IdeaKotlinSourceDependency.Type.Regular,
                coordinates = IdeaKotlinSourceCoordinates(
                    project = IdeaKotlinProjectCoordinates(projectComponentIdentifier),
                    sourceSetName = visibleSourceSetName
                )
            )
        }
    }
}
