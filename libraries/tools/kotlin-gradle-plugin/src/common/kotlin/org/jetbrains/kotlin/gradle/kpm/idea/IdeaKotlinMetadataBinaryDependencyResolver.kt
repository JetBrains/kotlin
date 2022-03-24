/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm.idea

import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.jetbrains.kotlin.gradle.plugin.mpp.MetadataDependencyResolution.ChooseVisibleSourceSets
import org.jetbrains.kotlin.gradle.plugin.mpp.MetadataDependencyResolution.ChooseVisibleSourceSets.MetadataProvider.JarMetadataProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.MetadataDependencyResolution.ChooseVisibleSourceSets.MetadataProvider.ProjectMetadataProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.FragmentGranularMetadataResolverFactory
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinGradleFragment
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinGradleModule.Companion.moduleName
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.toModuleDependency
import org.jetbrains.kotlin.gradle.utils.withTemporaryDirectory

internal class IdeaKotlinMetadataBinaryDependencyResolver(
    private val fragmentGranularMetadataResolverFactory: FragmentGranularMetadataResolverFactory
) : IdeaKotlinDependencyResolver {
    override fun resolve(fragment: KotlinGradleFragment): Set<IdeaKotlinDependency> {
        return fragmentGranularMetadataResolverFactory.getOrCreate(fragment).resolutions
            .filterIsInstance<ChooseVisibleSourceSets>()
            .flatMap { resolution -> resolve(fragment, resolution) }
            .toSet()
    }

    private fun resolve(fragment: KotlinGradleFragment, resolution: ChooseVisibleSourceSets): Iterable<IdeaKotlinDependency> {
        val gradleModuleIdentifier = resolution.dependency.id as? ModuleComponentIdentifier ?: return emptySet()
        val kotlinModuleIdentifier = resolution.dependency.toModuleDependency().moduleIdentifier

        /* Project to project metadata dependencies shall be resolved as source dependencies, somewhere else */
        val metadataProvider = when (resolution.metadataProvider) {
            is ProjectMetadataProvider -> return emptySet()
            is JarMetadataProvider -> resolution.metadataProvider
        }
        return resolution.allVisibleSourceSetNames.mapNotNull { visibleFragmentName ->
            val binaryFile = withTemporaryDirectory("metadataBinaryDependencyResolver") { temporaryDirectory ->
                val sourceBinaryFile = metadataProvider.getSourceSetCompiledMetadata(
                    sourceSetName = visibleFragmentName,
                    outputDirectory = temporaryDirectory,
                    materializeFile = true
                )

                if (!sourceBinaryFile.isFile)
                    return@mapNotNull null

                fragment.project.rootDir
                    .resolve(".gradle").resolve("kotlin").resolve("transformedKotlinMetadata")
                    .resolve(gradleModuleIdentifier.group)
                    .resolve(gradleModuleIdentifier.module)
                    .resolve(gradleModuleIdentifier.version)
                    .resolve(kotlinModuleIdentifier.moduleName)
                    .resolve(visibleFragmentName)
                    .resolve(sourceBinaryFile.name)
                    .apply { if (!isFile) sourceBinaryFile.copyTo(this) }
            }

            IdeaKotlinResolvedBinaryDependencyImpl(
                binaryType = IdeaKotlinDependency.CLASSPATH_BINARY_TYPE,
                binaryFile = binaryFile,
                coordinates = IdeaKotlinBinaryCoordinatesImpl(
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
