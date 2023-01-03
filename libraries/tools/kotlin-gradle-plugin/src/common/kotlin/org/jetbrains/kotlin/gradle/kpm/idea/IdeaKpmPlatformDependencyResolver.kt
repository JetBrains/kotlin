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
import org.jetbrains.kotlin.gradle.idea.kpm.*
import org.jetbrains.kotlin.gradle.kpm.idea.IdeaKpmPlatformDependencyResolver.*
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.*
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.GradleKpmConfigurationAttributesSetup
import org.jetbrains.kotlin.gradle.utils.markResolvable

/**
 * Resolves 'platform' binary dependencies for a given variant or fragment.
 * 'platform' binaries refer to actually linkable/executable artifacts (like .class files bundled as jar, or linkable native klibs)
 * This resolver is capable of resolving those artifacts even for non-variant "platform-like" fragments.
 * It will then use the [GradleKpmFragment.transitiveApiConfiguration] and [GradleKpmFragment.transitiveImplementationConfiguratione]'s
 * to resolve those binaries. See [IdeaKpmPlatformDependencyResolver.ArtifactResolution.PlatformFragment]
 */
class IdeaKpmPlatformDependencyResolver(
    private val binaryType: String = IdeaKpmDependency.CLASSPATH_BINARY_TYPE,
    private val artifactResolution: ArtifactResolution = ArtifactResolution.Variant()
) : IdeaKpmDependencyResolver {

    sealed class ArtifactResolution {
        /**
         * Resolve the artifacts from a [GradleKpmVariant] using its [GradleKpmVariant.compileDependenciesConfiguration],
         * which already knows how to resolve platform artifacts.
         * @param artifactViewAttributes: Additional attributes that will be used to create an [ArtifactView] for resolving the dependencies.
         */
        data class Variant(
            internal val artifactViewAttributes: GradleKpmConfigurationAttributesSetup<GradleKpmFragment> = GradleKpmConfigurationAttributesSetup.None
        ) : ArtifactResolution()

        /**
         * Capable of resolving artifacts from a plain [GradleKpmFragment] which does not have to implement [GradleKpmVariant].
         * Such fragments are called 'platform-like', since they still resolve the linkable platform dependencies.
         * @param platformResolutionAttributes: Attributes describing how to resolve platform artifacts in general.
         * @param artifactViewAttributes: Additional attributes that will be used to create an [ArtifactView] for
         * resolving the dependencies
         */
        data class PlatformFragment(
            internal val platformResolutionAttributes: GradleKpmConfigurationAttributesSetup<GradleKpmFragment>,
            internal val artifactViewAttributes: GradleKpmConfigurationAttributesSetup<GradleKpmFragment> = GradleKpmConfigurationAttributesSetup { },
        ) : ArtifactResolution()
    }

    override fun resolve(fragment: GradleKpmFragment): Set<IdeaKpmBinaryDependency> {
        val artifacts = artifactResolution.createArtifactView(fragment)?.artifacts ?: return emptySet()

        val unresolvedDependencies = artifacts.failures
            .onEach { reason -> fragment.project.logger.error("Failed to resolve dependency", reason) }
            .map { reason ->
                val selector = (reason as? ModuleVersionResolveException)?.selector as? ModuleComponentSelector
                /* Can't figure out the dependency here :( */
                    ?: return@map IdeaKpmUnresolvedBinaryDependencyImpl(
                        coordinates = null, cause = reason.message?.takeIf { it.isNotBlank() }
                    )

                IdeaKpmUnresolvedBinaryDependencyImpl(
                    coordinates = IdeaKpmBinaryCoordinatesImpl(selector.group, selector.module, selector.version),
                    cause = reason.message?.takeIf { it.isNotBlank() }
                )
            }.toSet()

        val resolvedDependencies = artifacts.artifacts.mapNotNull { artifact ->
            IdeaKpmResolvedBinaryDependencyImpl(
                coordinates = artifact.variant.owner.ideaKotlinBinaryCoordinates,
                binaryType = binaryType,
                binaryFile = artifact.file
            )
        }.toSet()

        return resolvedDependencies + unresolvedDependencies
    }
}

private val ComponentIdentifier.ideaKotlinBinaryCoordinates: IdeaKpmBinaryCoordinates?
    get() = when (this) {
        is ModuleComponentIdentifier -> IdeaKpmBinaryCoordinatesImpl(group, module, version)
        else -> null
    }

private fun ArtifactResolution.createArtifactView(fragment: GradleKpmFragment): ArtifactView? {
    return when (this) {
        is ArtifactResolution.Variant -> createVariantArtifactView(fragment)
        is ArtifactResolution.PlatformFragment -> createPlatformFragmentArtifactView(fragment)
    }
}

private fun ArtifactResolution.Variant.createVariantArtifactView(fragment: GradleKpmFragment): ArtifactView? {
    if (fragment !is GradleKpmVariant) return null
    return fragment.compileDependenciesConfiguration.incoming.artifactView { view ->
        view.isLenient = true
        view.componentFilter { id -> id !is ProjectComponentIdentifier }
        view.attributes.apply(artifactViewAttributes, fragment)
    }
}

private fun ArtifactResolution.PlatformFragment.createPlatformFragmentArtifactView(fragment: GradleKpmFragment): ArtifactView {
    val fragmentCompileDependencies = fragment.project.configurations.detachedConfiguration().markResolvable()

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
