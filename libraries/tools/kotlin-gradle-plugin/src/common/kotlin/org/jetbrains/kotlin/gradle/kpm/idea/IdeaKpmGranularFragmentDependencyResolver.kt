/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm.idea

import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.jetbrains.kotlin.gradle.plugin.mpp.MetadataDependencyResolution.ChooseVisibleSourceSets
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.GradleKpmFragmentGranularMetadataResolverFactory
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.GradleKpmFragment
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.GradleKpmModule.Companion.moduleName
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.currentBuildId
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.toKpmModuleDependency

internal class IdeaKpmGranularFragmentDependencyResolver(
    private val fragmentGranularMetadataResolverFactory: GradleKpmFragmentGranularMetadataResolverFactory
) : IdeaKpmDependencyResolver {
    override fun resolve(fragment: GradleKpmFragment): Set<IdeaKpmDependency> {
        return fragmentGranularMetadataResolverFactory.getOrCreate(fragment).resolutions
            .filterIsInstance<ChooseVisibleSourceSets>()
            .flatMap { resolution -> resolve(fragment, resolution) }
            .toSet()
    }

    private fun resolve(fragment: GradleKpmFragment, resolution: ChooseVisibleSourceSets): Iterable<IdeaKpmDependency> {
        val gradleProjectIdentifier = resolution.dependency.id as? ProjectComponentIdentifier ?: return emptyList()
        val kotlinModuleIdentifier = resolution.dependency.toKpmModuleDependency().moduleIdentifier
        return resolution.allVisibleSourceSetNames.map { visibleFragmentName ->
            IdeaKpmFragmentDependencyImpl(
                type = if (
                    gradleProjectIdentifier.build == fragment.project.currentBuildId() &&
                    gradleProjectIdentifier.projectPath == fragment.project.path
                ) IdeaKpmFragmentDependency.Type.Friend
                else IdeaKpmFragmentDependency.Type.Regular,
                coordinates = IdeaKpmFragmentCoordinatesImpl(
                    module = IdeaKpmModuleCoordinatesImpl(
                        buildId = gradleProjectIdentifier.build.name,
                        projectPath = gradleProjectIdentifier.projectPath,
                        projectName = gradleProjectIdentifier.projectName,
                        moduleName = kotlinModuleIdentifier.moduleName,
                        moduleClassifier = kotlinModuleIdentifier.moduleClassifier,
                    ),
                    fragmentName = visibleFragmentName
                )
            )
        }
    }
}
