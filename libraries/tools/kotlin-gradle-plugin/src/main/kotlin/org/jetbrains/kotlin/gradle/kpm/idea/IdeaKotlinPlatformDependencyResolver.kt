/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm.idea

import org.gradle.api.artifacts.ArtifactView
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ModuleComponentSelector
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.internal.resolve.ModuleVersionResolveException
import org.jetbrains.kotlin.gradle.kpm.idea.IdeaKotlinPlatformDependencyResolver.*
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.*
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.FragmentAttributes

class IdeaKotlinPlatformDependencyResolver(
    private val binaryType: String = IdeaKotlinDependency.CLASSPATH_BINARY_TYPE,
    private val artifactResolution: ArtifactResolution = ArtifactResolution.Variant()
) : IdeaKotlinDependencyResolver {

    sealed class ArtifactResolution {
        data class Variant(
            internal val artifactViewAttributes: FragmentAttributes<KotlinGradleFragment> = FragmentAttributes { }
        ) : ArtifactResolution()

        data class PlatformFragment(
            internal val platformResolutionAttributes: FragmentAttributes<KotlinGradleFragment>,
            internal val artifactViewAttributes: FragmentAttributes<KotlinGradleFragment> = FragmentAttributes { },
        ) : ArtifactResolution()
    }

    override fun resolve(fragment: KotlinGradleFragment): Set<IdeaKotlinBinaryDependency> {
        val artifacts = artifactResolution.createArtifactView(fragment)?.artifacts ?: return emptySet()

        val unresolvedDependencies = artifacts.failures
            .onEach { reason -> fragment.project.logger.error("Failed to resolve dependency", reason) }
            .map { reason ->
                val selector = (reason as? ModuleVersionResolveException)?.selector as? ModuleComponentSelector
                /* Can't figure out the dependency here :( */
                    ?: return@map IdeaKotlinUnresolvedBinaryDependencyImpl(
                        coordinates = null, cause = reason.message?.takeIf { it.isNotBlank() }
                    )

                IdeaKotlinUnresolvedBinaryDependencyImpl(
                    coordinates = IdeaKotlinBinaryCoordinatesImpl(selector.group, selector.module, selector.version),
                    cause = reason.message?.takeIf { it.isNotBlank() }
                )
            }.toSet()

        val resolvedDependencies = artifacts.artifacts.mapNotNull { artifact ->
            IdeaKotlinResolvedBinaryDependencyImpl(
                coordinates = artifact.variant.owner.ideaKotlinBinaryCoordinates,
                binaryType = binaryType,
                binaryFile = artifact.file
            )
        }.toSet()

        return resolvedDependencies + unresolvedDependencies
    }
}

private val ComponentIdentifier.ideaKotlinBinaryCoordinates: IdeaKotlinBinaryCoordinates?
    get() = when (this) {
        is ModuleComponentIdentifier -> IdeaKotlinBinaryCoordinatesImpl(group, module, version)
        else -> null
    }

private fun ArtifactResolution.createArtifactView(fragment: KotlinGradleFragment): ArtifactView? {
    return when (this) {
        is ArtifactResolution.Variant -> createVariantArtifactView(fragment)
        is ArtifactResolution.PlatformFragment -> createPlatformFragmentArtifactView(fragment)
    }
}

private fun ArtifactResolution.Variant.createVariantArtifactView(fragment: KotlinGradleFragment): ArtifactView? {
    if (fragment !is KotlinGradleVariant) return null
    return fragment.compileDependenciesConfiguration.incoming.artifactView { view ->
        view.isLenient = true
        view.componentFilter { id -> id !is ProjectComponentIdentifier }
        view.attributes.apply(artifactViewAttributes, fragment)
    }
}

private fun ArtifactResolution.PlatformFragment.createPlatformFragmentArtifactView(fragment: KotlinGradleFragment): ArtifactView {
    val fragmentCompileDependencies = fragment.project.configurations.detachedConfiguration()

    fragmentCompileDependencies.dependencies.addAll(
        fragment.transitiveApiConfiguration.allDependencies.matching { it !is ProjectDependency }
    )

    fragmentCompileDependencies.dependencies.addAll(
        fragment.transitiveImplementationConfiguration.allDependencies.matching { it !is ProjectDependency }
    )

    fragmentCompileDependencies.attributes.apply(platformResolutionAttributes, fragment)

    /* Ensure consistent dependency resolution result within the whole module */
    val allModuleCompileDependencies = fragment.project.configurations.getByName(
        fragment.containingModule.resolvableMetadataConfigurationName
    )
    fragmentCompileDependencies.shouldResolveConsistentlyWith(allModuleCompileDependencies)


    return fragmentCompileDependencies.incoming.artifactView { view ->
        view.isLenient = true
        view.componentFilter { id -> id !is ProjectComponentIdentifier }
        view.attributes.apply(artifactViewAttributes, fragment)
    }
}
