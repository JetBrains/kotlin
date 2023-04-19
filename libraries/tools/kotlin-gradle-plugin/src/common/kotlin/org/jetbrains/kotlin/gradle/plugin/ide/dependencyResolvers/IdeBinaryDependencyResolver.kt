/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.ide.dependencyResolvers

import org.gradle.api.artifacts.ArtifactView
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.DependencySubstitutions
import org.gradle.api.artifacts.component.*
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.internal.component.local.model.OpaqueComponentArtifactIdentifier
import org.gradle.internal.resolve.ModuleVersionResolveException
import org.jetbrains.kotlin.gradle.ExternalKotlinTargetApi
import org.jetbrains.kotlin.gradle.idea.tcs.*
import org.jetbrains.kotlin.gradle.idea.tcs.extras.artifactsClasspath
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.ide.IdeDependencyResolver
import org.jetbrains.kotlin.gradle.plugin.ide.IdeDependencyResolver.Companion.gradleArtifact
import org.jetbrains.kotlin.gradle.plugin.ide.IdeaKotlinBinaryCoordinates
import org.jetbrains.kotlin.gradle.plugin.ide.IdeaKotlinProjectCoordinates
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMetadataCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.KotlinCompilationConfigurationsContainer
import org.jetbrains.kotlin.gradle.plugin.mpp.internal
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.GradleKpmFragment
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.GradleKpmVariant
import org.jetbrains.kotlin.gradle.plugin.mpp.resolvableMetadataConfiguration
import org.jetbrains.kotlin.gradle.plugin.sources.DefaultKotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.sources.InternalKotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.sources.internal
import org.jetbrains.kotlin.gradle.utils.markResolvable
import org.gradle.api.artifacts.ResolutionStrategy
import org.jetbrains.kotlin.tooling.core.mutableExtrasOf

/**
 * Dependency resolver for [IdeaKotlinBinaryDependency] instances:
 * This resolver is intended to resolve dependencies from maven repositories by providing a specific artifact view
 *
 * @param binaryType Binary type used when creating [IdeaKotlinResolvedBinaryDependency.binaryType] from resolved artifacts.
 * Default is [IdeaKotlinBinaryDependency.KOTLIN_COMPILE_BINARY_TYPE] to indicate binary dependencies for the Kotlin compiler
 * such as .jar or .klib files
 *
 * @param artifactResolutionStrategy Strategy passed for creating a resolvable artifactView for a given source set.
 * see
 * - [ArtifactResolutionStrategy.Compilation],
 * - [ArtifactResolutionStrategy.ResolvableConfiguration],
 * - [ArtifactResolutionStrategy.PlatformLikeSourceSet]
 *
 * Default is: [ArtifactResolutionStrategy.Compilation] which will find the most suitable compilation and resolve dependencies
 * from the given [KotlinCompilationConfigurationsContainer.compileDependencyConfiguration]
 */
@ExternalKotlinTargetApi
class IdeBinaryDependencyResolver @JvmOverloads constructor(
    private val binaryType: String = IdeaKotlinBinaryDependency.KOTLIN_COMPILE_BINARY_TYPE,
    private val artifactResolutionStrategy: ArtifactResolutionStrategy = ArtifactResolutionStrategy.Compilation()
) : IdeDependencyResolver {

    @ExternalKotlinTargetApi
    sealed class ArtifactResolutionStrategy {

        /**
         * Resolve the artifacts from a [KotlinSourceSet] using its [KotlinCompilation.compileDependencyConfigurationName],
         * which already knows how to resolve platform artifacts.
         *
         * @param compilationSelector: Selects the compilation used for resolving dependencies for a given source set
         * default: Find a single 'platform' compilation
         * @param setupArtifactViewAttributes: Additional attributes that will be used to create an [ArtifactView] for resolving the dependencies.
         * @param componentFilter: Filter added to the artifactView: Only components passing the filter will be resolved
         */
        @ExternalKotlinTargetApi
        class Compilation @JvmOverloads constructor(
            internal val compilationSelector: (KotlinSourceSet) -> KotlinCompilation<*>? = { sourceSet -> sourceSet.internal.compilations.singleOrNull { it.platformType != KotlinPlatformType.common } },
            internal val setupArtifactViewAttributes: AttributeContainer.(sourceSet: KotlinSourceSet) -> Unit = {},
            internal val componentFilter: ((ComponentIdentifier) -> Boolean)? = null
        ) : ArtifactResolutionStrategy()

        /**
         * Resolve the artifacts from a [KotlinSourceSet] using the configuration returned by [configurationSelector].
         * @param configurationSelector Returns the configuration that shall be resolved for the given [KotlinSourceSet]
         * @param setupArtifactViewAttributes: Additional attributes that will be used to create an [ArtifactView] for resolving the dependencies.
         * @param componentFilter Filter added to the artifactView: Only components passing the filter will be resolved
         */
        @ExternalKotlinTargetApi
        class ResolvableConfiguration @JvmOverloads constructor(
            internal val configurationSelector: (KotlinSourceSet) -> Configuration?,
            internal val setupArtifactViewAttributes: AttributeContainer.(sourceSet: KotlinSourceSet) -> Unit = {},
            internal val componentFilter: ((ComponentIdentifier) -> Boolean)? = null
        ) : ArtifactResolutionStrategy()

        /**
         * Capable of resolving artifacts from a plain [GradleKpmFragment] which does not have to implement [GradleKpmVariant].
         * Such fragments are called 'platform-like', since they still resolve the linkable platform dependencies.
         * @param setupPlatformResolutionAttributes: Attributes describing how to resolve platform artifacts in general.
         * @param setupArtifactViewAttributes: Additional attributes that will be used to create an [ArtifactView] for
         * resolving the dependencies
         * @param componentFilter Filter added to the artifactView: Only components passing the filter will be resolved
         * @param dependencySubstitution Dependency substitution added to the adhoc configuration created for this resolution.
         * see [ResolutionStrategy.dependencySubstitution]
         */
        @ExternalKotlinTargetApi
        class PlatformLikeSourceSet @JvmOverloads constructor(
            internal val setupPlatformResolutionAttributes: AttributeContainer.(sourceSet: KotlinSourceSet) -> Unit,
            internal val setupArtifactViewAttributes: AttributeContainer.(sourceSet: KotlinSourceSet) -> Unit = {},
            internal val componentFilter: ((ComponentIdentifier) -> Boolean)? = null,
            internal val dependencySubstitution: ((DependencySubstitutions) -> Unit)? = null,
        ) : ArtifactResolutionStrategy()
    }

    override fun resolve(sourceSet: KotlinSourceSet): Set<IdeaKotlinDependency> {
        val artifacts = artifactResolutionStrategy.createArtifactView(sourceSet.internal)?.artifacts ?: return emptySet()

        val unresolvedDependencies = artifacts.failures
            .onEach { reason -> sourceSet.project.logger.error("Failed to resolve platform dependency on ${sourceSet.name}", reason) }
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
            when (val componentId = artifact.id.componentIdentifier) {
                is ProjectComponentIdentifier -> {
                    IdeaKotlinProjectArtifactDependency(
                        type = IdeaKotlinSourceDependency.Type.Regular, coordinates = IdeaKotlinProjectCoordinates(componentId)
                    ).apply {
                        artifactsClasspath.add(artifact.file)
                    }
                }

                is ModuleComponentIdentifier -> {
                    IdeaKotlinResolvedBinaryDependency(
                        coordinates = IdeaKotlinBinaryCoordinates(componentId),
                        binaryType = binaryType,
                        classpath = IdeaKotlinClasspath(artifact.file),
                    )
                }

                is LibraryBinaryIdentifier -> {
                    IdeaKotlinResolvedBinaryDependency(
                        binaryType = binaryType, coordinates = IdeaKotlinBinaryCoordinates(
                            group = componentId.projectPath + "(${componentId.variant})",
                            module = componentId.libraryName,
                            version = null,
                            sourceSetName = null
                        ), classpath = IdeaKotlinClasspath(artifact.file)
                    )
                }

                is OpaqueComponentArtifactIdentifier -> {
                    /* Such dependencies *would* require implementing a resolver */
                    null
                }

                else -> {
                    logger.warn("Unhandled componentId: ${componentId.javaClass}")
                    null
                }
            }?.also { dependency -> dependency.gradleArtifact = artifact }
        }.toSet()

        return resolvedDependencies + unresolvedDependencies
    }

    private fun ArtifactResolutionStrategy.createArtifactView(sourceSet: InternalKotlinSourceSet): ArtifactView? {
        return when (this) {
            is ArtifactResolutionStrategy.Compilation -> createArtifactView(sourceSet)
            is ArtifactResolutionStrategy.ResolvableConfiguration -> createArtifactView(sourceSet)
            is ArtifactResolutionStrategy.PlatformLikeSourceSet -> createArtifactView(sourceSet)
        }
    }

    private fun ArtifactResolutionStrategy.Compilation.createArtifactView(sourceSet: InternalKotlinSourceSet): ArtifactView? {
        val compilation = compilationSelector(sourceSet) ?: return null

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
            view.attributes.setupArtifactViewAttributes(sourceSet)
            if (componentFilter != null) {
                view.componentFilter(componentFilter)
            }
        }
    }

    private fun ArtifactResolutionStrategy.ResolvableConfiguration.createArtifactView(sourceSet: InternalKotlinSourceSet): ArtifactView? {
        val configuration = configurationSelector(sourceSet) ?: return null
        return configuration.incoming.artifactView { view ->
            view.isLenient = true
            view.attributes.setupArtifactViewAttributes(sourceSet)
            if (componentFilter != null) {
                view.componentFilter(componentFilter)
            }
        }
    }

    private fun ArtifactResolutionStrategy.PlatformLikeSourceSet.createArtifactView(sourceSet: InternalKotlinSourceSet): ArtifactView? {
        if (sourceSet !is DefaultKotlinSourceSet) return null
        val project = sourceSet.project

        val platformLikeCompileDependenciesConfiguration = project.configurations.detachedConfiguration()
        platformLikeCompileDependenciesConfiguration.markResolvable()
        platformLikeCompileDependenciesConfiguration.attributes.setupPlatformResolutionAttributes(sourceSet)
        platformLikeCompileDependenciesConfiguration.dependencies.addAll(sourceSet.resolvableMetadataConfiguration.allDependencies)

        if (dependencySubstitution != null) {
            platformLikeCompileDependenciesConfiguration.resolutionStrategy.dependencySubstitution(dependencySubstitution)
        }

        return platformLikeCompileDependenciesConfiguration.incoming.artifactView { view ->
            view.isLenient = true
            view.attributes.setupArtifactViewAttributes(sourceSet)
            if (componentFilter != null) {
                view.componentFilter(componentFilter)
            }
        }
    }

    private companion object {
        val logger: Logger = Logging.getLogger(IdeBinaryDependencyResolver::class.java)
    }
}
