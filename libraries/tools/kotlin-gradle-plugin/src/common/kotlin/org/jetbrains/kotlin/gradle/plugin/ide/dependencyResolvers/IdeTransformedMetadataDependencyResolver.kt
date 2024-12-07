/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.ide.dependencyResolvers

import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.jetbrains.kotlin.gradle.idea.tcs.*
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.ide.IdeDependencyResolver
import org.jetbrains.kotlin.gradle.plugin.mpp.MetadataDependencyResolution.ChooseVisibleSourceSets
import org.jetbrains.kotlin.gradle.plugin.mpp.MetadataDependencyResolution.ChooseVisibleSourceSets.MetadataProvider.ArtifactMetadataProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.kotlinTransformedMetadataLibraryDirectoryForIde
import org.jetbrains.kotlin.gradle.plugin.mpp.read
import org.jetbrains.kotlin.gradle.plugin.sources.internal
import org.jetbrains.kotlin.tooling.core.mutableExtrasOf

internal object IdeTransformedMetadataDependencyResolver : IdeDependencyResolver {
    override fun resolve(sourceSet: KotlinSourceSet): Set<IdeaKotlinDependency> =
        sourceSet.resolveMetadata<ChooseVisibleSourceSets>()
            .filter { resolution -> resolution.dependency.id !is ProjectComponentIdentifier }
            .flatMap { resolution -> resolve(sourceSet, resolution) }
            .toSet()

    private fun resolve(sourceSet: KotlinSourceSet, resolution: ChooseVisibleSourceSets): Iterable<IdeaKotlinDependency> {
        val metadataProvider = resolution.metadataProvider as? ArtifactMetadataProvider ?: return emptySet()
        return metadataProvider.read { artifactContent ->
            resolution.allVisibleSourceSetNames.mapNotNull { visibleSourceSet ->
                val sourceSetContent = artifactContent.findSourceSet(visibleSourceSet) ?: return@mapNotNull null
                val sourceSetMetadataBinary = sourceSetContent.metadataBinary ?: return@mapNotNull null

                val metadataLibraryOutputFile = sourceSet.internal.project.kotlinTransformedMetadataLibraryDirectoryForIde
                    .resolve(sourceSetMetadataBinary.relativeFile)

                metadataLibraryOutputFile.parentFile.mkdirs()
                if (!metadataLibraryOutputFile.exists()) {
                    sourceSetMetadataBinary.copyTo(metadataLibraryOutputFile)
                    if (!metadataLibraryOutputFile.exists()) return@mapNotNull null
                }

                IdeaKotlinResolvedBinaryDependency(
                    binaryType = IdeaKotlinBinaryDependency.KOTLIN_COMPILE_BINARY_TYPE,
                    classpath = IdeaKotlinClasspath(metadataLibraryOutputFile),
                    extras = mutableExtrasOf(),
                    coordinates = IdeaKotlinBinaryCoordinates(
                        group = metadataProvider.moduleDependencyIdentifier.groupId ?: "",
                        module = metadataProvider.moduleDependencyIdentifier.moduleId,
                        version = metadataProvider.moduleDependencyVersion,
                        sourceSetName = visibleSourceSet
                    )
                )
            }
        }
    }
}
