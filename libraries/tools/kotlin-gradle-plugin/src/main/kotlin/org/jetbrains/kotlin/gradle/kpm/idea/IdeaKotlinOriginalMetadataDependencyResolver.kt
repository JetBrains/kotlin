/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm.idea

import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.MetadataDependencyResolution.KeepOriginalDependency
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.FragmentGranularMetadataResolverFactory
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinGradleFragment
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.resolvableMetadataConfigurationName

internal class IdeaKotlinOriginalMetadataDependencyResolver(
    private val fragmentGranularMetadataResolverFactory: FragmentGranularMetadataResolverFactory
) : IdeaKotlinDependencyResolver {
    override fun resolve(fragment: KotlinGradleFragment): Set<IdeaKotlinDependency> {
        val dependencyIdentifiers = fragmentGranularMetadataResolverFactory.getOrCreate(fragment).resolutions
            .filterIsInstance<KeepOriginalDependency>()
            .mapNotNull { resolution -> resolution.dependency.id as? ModuleComponentIdentifier }
            .toSet()

        val allModuleCompileDependenciesConfiguration = fragment.project.configurations
            .getByName(fragment.containingModule.resolvableMetadataConfigurationName)

        return allModuleCompileDependenciesConfiguration.incoming.artifactView { view ->
            view.componentFilter { id -> id in dependencyIdentifiers }
            view.isLenient = true
        }.artifacts
            .map { artifact ->
                val artifactId = artifact.variant.owner as ModuleComponentIdentifier
                IdeaKotlinResolvedBinaryDependencyImpl(
                    binaryType = IdeaKotlinDependency.CLASSPATH_BINARY_TYPE,
                    binaryFile = artifact.file,
                    coordinates = IdeaKotlinBinaryCoordinatesImpl(artifactId.group, artifactId.module, artifactId.version)
                )
            }.toSet()
    }
}
