/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm.idea

import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.jetbrains.kotlin.gradle.plugin.mpp.MetadataDependencyResolution.ChooseVisibleSourceSets
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.FragmentGranularMetadataResolverFactory
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinGradleFragment
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinGradleModule.Companion.moduleName
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.toModuleDependency

internal class IdeaKotlinSourceDependencyResolver(
    private val fragmentGranularMetadataResolverFactory: FragmentGranularMetadataResolverFactory
) : IdeaKotlinDependencyResolver {
    override fun resolve(fragment: KotlinGradleFragment): Set<IdeaKotlinDependency> {
        return fragmentGranularMetadataResolverFactory.getOrCreate(fragment).resolutions
            .filterIsInstance<ChooseVisibleSourceSets>()
            .flatMap { resolution -> resolve(resolution) }
            .toSet()
    }

    private fun resolve(resolution: ChooseVisibleSourceSets): Iterable<IdeaKotlinDependency> {
        val gradleProjectIdentifier = resolution.dependency.id as? ProjectComponentIdentifier ?: return emptyList()
        val kotlinModuleIdentifier = resolution.dependency.toModuleDependency().moduleIdentifier
        return resolution.allVisibleSourceSetNames.map { visibleFragmentName ->
            IdeaKotlinSourceDependencyImpl(
                buildId = gradleProjectIdentifier.build.name,
                projectPath = gradleProjectIdentifier.projectPath,
                projectName = gradleProjectIdentifier.projectName,
                kotlinModuleName = kotlinModuleIdentifier.moduleName,
                kotlinModuleClassifier = kotlinModuleIdentifier.moduleClassifier,
                kotlinFragmentName = visibleFragmentName
            )
        }
    }
}
