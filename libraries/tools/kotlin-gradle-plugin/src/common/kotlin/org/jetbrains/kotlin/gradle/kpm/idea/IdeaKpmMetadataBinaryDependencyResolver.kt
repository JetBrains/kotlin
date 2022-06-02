/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm.idea

import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.jetbrains.kotlin.gradle.plugin.mpp.MetadataDependencyResolution.ChooseVisibleSourceSets
import org.jetbrains.kotlin.gradle.plugin.mpp.MetadataDependencyResolution.ChooseVisibleSourceSets.MetadataProvider.JarMetadataProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.MetadataDependencyResolution.ChooseVisibleSourceSets.MetadataProvider.ProjectMetadataProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.GradleKpmFragment
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.GradleKpmFragmentGranularMetadataResolver
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.GradleKpmModule.Companion.moduleName
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.toKpmModuleDependency
import org.jetbrains.kotlin.gradle.utils.withTemporaryDirectory

internal class IdeaKpmMetadataBinaryDependencyResolver : IdeaKpmDependencyResolver {
    override fun resolve(fragment: GradleKpmFragment): Set<IdeaKpmDependency> {
        return GradleKpmFragmentGranularMetadataResolver.getForModule(fragment.containingModule)
            .getMetadataDependenciesForFragment(fragment)
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

            IdeaKpmResolvedBinaryDependencyImpl(
                binaryType = IdeaKpmDependency.CLASSPATH_BINARY_TYPE,
                binaryFile = binaryFile,
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
