/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.ide.dependencyResolvers

import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinDependency
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinSourceCoordinates
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinSourceDependency
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.ide.IdeDependencyResolver
import org.jetbrains.kotlin.gradle.plugin.mpp.MetadataDependencyResolution
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.currentBuildId
import org.jetbrains.kotlin.gradle.plugin.sources.DefaultKotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.sources.KotlinDependencyScope.*
import org.jetbrains.kotlin.tooling.core.mutableExtrasOf

object IdeVisibleMultiplatformSourceDependencyResolver : IdeDependencyResolver {
    override fun resolve(sourceSet: KotlinSourceSet): Set<IdeaKotlinDependency> {
        if (sourceSet !is DefaultKotlinSourceSet) return emptySet()
        return sourceSet.dependencyTransformations[API_SCOPE]?.metadataDependencyResolutions?.toList().orEmpty()
            .plus(sourceSet.dependencyTransformations[IMPLEMENTATION_SCOPE]?.metadataDependencyResolutions?.toList().orEmpty())
            .plus(sourceSet.dependencyTransformations[COMPILE_ONLY_SCOPE]?.metadataDependencyResolutions?.toList().orEmpty())
            .filterIsInstance<MetadataDependencyResolution.ChooseVisibleSourceSets>()
            .flatMap { resolution -> resolveSourceDependencies(resolution) }
            .toSet()
    }

    private fun resolveSourceDependencies(
        resolution: MetadataDependencyResolution.ChooseVisibleSourceSets
    ): Iterable<IdeaKotlinDependency> {
        val project = resolution.projectDependency ?: return emptyList()
        return resolution.allVisibleSourceSetNames.map { visibleSourceSetName ->
            IdeaKotlinSourceDependency(
                type = IdeaKotlinSourceDependency.Type.Regular,
                extras = mutableExtrasOf(),
                coordinates = IdeaKotlinSourceCoordinates(
                    buildId = project.currentBuildId().toString(),
                    projectPath = project.path,
                    projectName = project.name,
                    sourceSetName = visibleSourceSetName
                )
            )
        }
    }
}
