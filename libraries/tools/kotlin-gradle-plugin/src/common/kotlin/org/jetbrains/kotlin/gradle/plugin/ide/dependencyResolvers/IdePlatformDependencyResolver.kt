/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.ide.dependencyResolvers

import org.gradle.api.artifacts.ArtifactView
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ModuleComponentSelector
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.logging.Logging
import org.gradle.internal.resolve.ModuleVersionResolveException
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinBinaryCoordinates
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinDependency
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinResolvedBinaryDependency
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinUnresolvedBinaryDependency
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.ide.IdeDependencyResolver
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMetadataCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.internal
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.GradleKpmFragment
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.GradleKpmVariant
import org.jetbrains.kotlin.gradle.plugin.sources.*
import org.jetbrains.kotlin.gradle.targets.metadata.ALL_COMPILE_METADATA_CONFIGURATION_NAME
import org.jetbrains.kotlin.tooling.core.mutableExtrasOf

internal class IdePlatformDependencyResolver(
    private val binaryType: String = IdeaKotlinDependency.CLASSPATH_BINARY_TYPE,
    private val artifactResolutionStrategy: ArtifactResolutionStrategy = ArtifactResolutionStrategy.Compilation()
) : IdeDependencyResolver {

    sealed class ArtifactResolutionStrategy {
        /**
         * Resolve the artifacts from a [GradleKpmVariant] using its [GradleKpmVariant.compileDependenciesConfiguration],
         * which already knows how to resolve platform artifacts.
         * @param setupArtifactViewAttributes: Additional attributes that will be used to create an [ArtifactView] for resolving the dependencies.
         */
        data class Compilation(
            internal val compilationSelector: (Set<KotlinCompilation<*>>) -> KotlinCompilation<*>? = { it.singleOrNull() },
            internal val setupArtifactViewAttributes: AttributeContainer.(sourceSet: KotlinSourceSet) -> Unit = {}
        ) : ArtifactResolutionStrategy()

        /**
         * Capable of resolving artifacts from a plain [GradleKpmFragment] which does not have to implement [GradleKpmVariant].
         * Such fragments are called 'platform-like', since they still resolve the linkable platform dependencies.
         * @param setupPlatformResolutionAttributes: Attributes describing how to resolve platform artifacts in general.
         * @param setupArtifactViewAttributes: Additional attributes that will be used to create an [ArtifactView] for
         * resolving the dependencies
         */
        data class PlatformLikeSourceSet(
            internal val setupPlatformResolutionAttributes: AttributeContainer.(sourceSet: KotlinSourceSet) -> Unit,
            internal val setupArtifactViewAttributes: AttributeContainer.(sourceSet: KotlinSourceSet) -> Unit = {},
        ) : ArtifactResolutionStrategy()
    }

    override fun resolve(sourceSet: KotlinSourceSet): Set<IdeaKotlinDependency> {
        val artifacts = artifactResolutionStrategy.createArtifactView(sourceSet.internal)?.artifacts ?: return emptySet()

        val unresolvedDependencies = artifacts.failures
            .onEach { reason -> sourceSet.project.logger.error("Failed to resolve dependency", reason) }
            .map { reason ->
                val selector = (reason as? ModuleVersionResolveException)?.selector as? ModuleComponentSelector
                /* Can't figure out the dependency here :( */
                    ?: return@map IdeaKotlinUnresolvedBinaryDependency(
                        coordinates = null, cause = reason.message?.takeIf { it.isNotBlank() }, extras = mutableExtrasOf()
                    )

                IdeaKotlinUnresolvedBinaryDependency(
                    coordinates = IdeaKotlinBinaryCoordinates(selector.group, selector.module, selector.version, null),
                    cause = reason.message?.takeIf { it.isNotBlank() },
                    extras = mutableExtrasOf()
                )
            }.toSet()

        val resolvedDependencies = artifacts.artifacts.mapNotNull { artifact ->
            IdeaKotlinResolvedBinaryDependency(
                coordinates = artifact.variant.owner.toIdeaKotlinBinaryCoordinatesOrNull(),
                binaryType = binaryType,
                binaryFile = artifact.file,
                extras = mutableExtrasOf()
            )
        }.toSet()

        return resolvedDependencies + unresolvedDependencies
    }

    private fun ArtifactResolutionStrategy.createArtifactView(sourceSet: InternalKotlinSourceSet): ArtifactView? {
        return when (this) {
            is ArtifactResolutionStrategy.Compilation -> createArtifactView(sourceSet)
            is ArtifactResolutionStrategy.PlatformLikeSourceSet -> createArtifactView(sourceSet)
        }
    }

    private fun ArtifactResolutionStrategy.Compilation.createArtifactView(sourceSet: InternalKotlinSourceSet): ArtifactView? {
        val compilation = compilationSelector(sourceSet.compilations) ?: return null

        /*
        Prevent case where this resolver was configured to resolve dependencies for a metadata compilation:
        Refuse resolution. Write your own code if you really want to do this!
         */
        if (compilation is KotlinMetadataCompilation<*>) {
            logger.warn("Unexpected ${KotlinMetadataCompilation::class.java}(${compilation.name}) for $sourceSet")
            return null
        }

        return compilation.internal.configurations.compileDependencyConfiguration.incoming.artifactView { view ->
            view.isLenient = true
            view.componentFilter { id -> id is ModuleComponentIdentifier }
            view.attributes.setupArtifactViewAttributes(sourceSet)
        }
    }

    private fun ArtifactResolutionStrategy.PlatformLikeSourceSet.createArtifactView(sourceSet: InternalKotlinSourceSet): ArtifactView? {
        if (sourceSet !is DefaultKotlinSourceSet) return null
        val sourceSetCompileDependencies = sourceSet.project.configurations.detachedConfiguration()
        sourceSetCompileDependencies.attributes.setupPlatformResolutionAttributes(sourceSet)

        ((sourceSet.getAdditionalVisibleSourceSets() + sourceSet).withDependsOnClosure).forEach { visibleSourceSet ->
            sourceSetCompileDependencies.dependencies.addAll(
                sourceSet.project.configurations.getByName(visibleSourceSet.apiConfigurationName).allDependencies
            )

            sourceSetCompileDependencies.dependencies.addAll(
                sourceSet.project.configurations.getByName(visibleSourceSet.implementationConfigurationName).allDependencies
            )

            sourceSetCompileDependencies.dependencies.addAll(
                sourceSet.project.configurations.getByName(visibleSourceSet.compileOnlyConfigurationName).allDependencies
            )
        }

        /* Ensure consistent dependency resolution result within the whole module */
        sourceSetCompileDependencies.shouldResolveConsistentlyWith(
            sourceSet.project.configurations.getByName(ALL_COMPILE_METADATA_CONFIGURATION_NAME)
        )

        return sourceSetCompileDependencies.incoming.artifactView { view ->
            view.isLenient = true
            view.componentFilter { id -> id is ModuleComponentIdentifier }
            view.attributes.setupArtifactViewAttributes(sourceSet)
        }
    }

    private fun ComponentIdentifier.toIdeaKotlinBinaryCoordinatesOrNull(): IdeaKotlinBinaryCoordinates? {
        return when (this) {
            is ModuleComponentIdentifier -> IdeaKotlinBinaryCoordinates(group, module, version)
            else -> null
        }
    }

    private companion object {
        val logger = Logging.getLogger(IdePlatformDependencyResolver::class.java)
    }
}
