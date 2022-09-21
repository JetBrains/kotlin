/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm.idea

import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.jetbrains.kotlin.gradle.plugin.mpp.MetadataDependencyResolution.ChooseVisibleSourceSets
import org.jetbrains.kotlin.gradle.plugin.mpp.MetadataDependencyResolution.ChooseVisibleSourceSets.MetadataProvider.ArtifactMetadataProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.MetadataDependencyResolution.ChooseVisibleSourceSets.MetadataProvider.ProjectMetadataProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.kotlinTransformedMetadataLibraryDirectoryForIde
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.GradleKpmFragment
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.GradleKpmFragmentGranularMetadataResolverFactory
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.GradleKpmModule.Companion.moduleName
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.toKpmModuleDependency

internal class IdeaKpmMetadataBinaryDependencyResolver(
    private val fragmentGranularMetadataResolverFactory: GradleKpmFragmentGranularMetadataResolverFactory
) : IdeaKpmDependencyResolver {
    override fun resolve(fragment: GradleKpmFragment): Set<IdeaKpmDependency> {
        return fragmentGranularMetadataResolverFactory.getOrCreate(fragment).resolutions
            .filterIsInstance<ChooseVisibleSourceSets>()
            .flatMap { resolution -> resolve(fragment, resolution) }
            .toSet()
    }

    private fun resolve(fragment: GradleKpmFragment, resolution: ChooseVisibleSourceSets): Iterable<IdeaKpmDependency> {
        val gradleModuleIdentifier = resolution.dependency.id as? ModuleComponentIdentifier ?: return emptySet()
        val kotlinModuleIdentifier = resolution.dependency.toKpmModuleDependency().moduleIdentifier

        /* Project to project metadata dependencies shall be resolved as source dependencies, somewhere else */
        val metadataProvider = when (resolution.metadataProvider) {
            is ProjectMetadataProvider -> return emptySet()
            is ArtifactMetadataProvider -> resolution.metadataProvider
        }

        return metadataProvider.read { artifactContent ->
            resolution.allVisibleSourceSetNames.mapNotNull { visibleFragmentName ->
                val sourceSetContent = artifactContent.findSourceSet(visibleFragmentName) ?: return@mapNotNull null
                val metadataBinary = sourceSetContent.metadataBinary ?: return@mapNotNull null

                val libraryFile = fragment.project.kotlinTransformedMetadataLibraryDirectoryForIde
                    .resolve(metadataBinary.relativeFile)
                    .apply { if (!isFile) metadataBinary.copyTo(this) }

                IdeaKpmResolvedBinaryDependencyImpl(
                    binaryType = IdeaKpmDependency.CLASSPATH_BINARY_TYPE,
                    binaryFile = libraryFile,
                    coordinates = IdeaKpmBinaryCoordinatesImpl(
                        group = gradleModuleIdentifier.group,
                        module = gradleModuleIdentifier.module,
                        version = gradleModuleIdentifier.version,
                        kotlinModuleName = kotlinModuleIdentifier.moduleName,
                        kotlinFragmentName = visibleFragmentName
                    )
                )
            }
        }
    }
}
