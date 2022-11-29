/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.ide.dependencyResolvers

import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinDependency
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinResolvedBinaryDependency
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.ide.IdeDependencyResolver
import org.jetbrains.kotlin.gradle.plugin.ide.IdeaKotlinBinaryCoordinates
import org.jetbrains.kotlin.gradle.plugin.mpp.MetadataDependencyResolution
import org.jetbrains.kotlin.gradle.plugin.mpp.resolvableMetadataConfiguration
import org.jetbrains.kotlin.gradle.plugin.sources.project

object IdeOriginalMetadataDependencyResolver : IdeDependencyResolver {
    override fun resolve(sourceSet: KotlinSourceSet): Set<IdeaKotlinDependency> {
        val metadataDependenciesConfiguration = resolvableMetadataConfiguration(sourceSet.project, listOf(sourceSet),)

        val keptOriginalDependencyResolutionIds = sourceSet.resolveMetadata<MetadataDependencyResolution.KeepOriginalDependency>()
            .map { it.dependency.id }.toSet()

        val artifactsView = metadataDependenciesConfiguration.incoming.artifactView { view ->
            view.componentFilter { id -> id in keptOriginalDependencyResolutionIds }
            view.isLenient = true
        }

        return artifactsView.artifacts.mapNotNull { artifact ->
            val moduleId = artifact.id.componentIdentifier as? ModuleComponentIdentifier ?: return@mapNotNull null
            IdeaKotlinResolvedBinaryDependency(
                binaryType = IdeaKotlinDependency.CLASSPATH_BINARY_TYPE,
                binaryFile = artifact.file,
                coordinates = IdeaKotlinBinaryCoordinates(moduleId)
            )
        }.toSet()
    }
}