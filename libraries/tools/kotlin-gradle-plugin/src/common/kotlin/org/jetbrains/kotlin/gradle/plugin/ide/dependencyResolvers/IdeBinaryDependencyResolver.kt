/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.ide.dependencyResolvers

import org.gradle.api.artifacts.*
import org.gradle.api.artifacts.component.*
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.internal.component.local.model.OpaqueComponentArtifactIdentifier
import org.gradle.internal.resolve.ModuleVersionResolveException
import org.jetbrains.kotlin.gradle.ExternalKotlinTargetApi
import org.jetbrains.kotlin.gradle.idea.tcs.*
import org.jetbrains.kotlin.gradle.idea.tcs.extras.artifactsClasspath
import org.jetbrains.kotlin.gradle.idea.tcs.extras.isOpaqueFileDependency
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.ide.*
import org.jetbrains.kotlin.gradle.plugin.ide.IdeDependencyResolver.Companion.gradleArtifact
import org.jetbrains.kotlin.gradle.plugin.ide.dependencyResolvers.IdeBinaryDependencyResolver.ArtifactResolutionStrategy
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMetadataCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.KotlinCompilationConfigurationsContainer
import org.jetbrains.kotlin.gradle.plugin.mpp.internal
import org.jetbrains.kotlin.gradle.plugin.mpp.resolvableMetadataConfiguration
import org.jetbrains.kotlin.gradle.plugin.sources.DefaultKotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.sources.InternalKotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.sources.internal
import org.jetbrains.kotlin.gradle.utils.detachedResolvable
import org.jetbrains.kotlin.gradle.utils.relativeOrAbsolute
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
    private val artifactResolutionStrategy: ArtifactResolutionStrategy = ArtifactResolutionStrategy.Compilation(),
) : IdeDependencyResolver {

    @ExternalKotlinTargetApi
    sealed class ArtifactResolutionStrategy {
        internal abstract val setupArtifactViewAttributes: AttributeContainer.(sourceSet: KotlinSourceSet) -> Unit
        internal abstract val componentFilter: ((ComponentIdentifier) -> Boolean)?
        internal abstract val dependencyFilter: ((Dependency) -> Boolean)?

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
            internal val compilationSelector: (KotlinSourceSet) -> KotlinCompilation<*>? =
                { sourceSet -> sourceSet.internal.compilations.singleOrNull { it.platformType != KotlinPlatformType.common } },
            override val setupArtifactViewAttributes: AttributeContainer.(sourceSet: KotlinSourceSet) -> Unit = {},
            override val componentFilter: ((ComponentIdentifier) -> Boolean)? = null,
            override val dependencyFilter: ((Dependency) -> Boolean)? = null,
        ) : ArtifactResolutionStrategy()

        /**
         * Resolve the artifacts from a [KotlinSourceSet] using the configuration returned by [configurationSelector].
         * @param configurationSelector Returns the configuration that shall be resolved for the given [KotlinSourceSet]
         * @param setupArtifactViewAttributes: Additional attributes that will be used to create an [ArtifactView] for resolving the dependencies.
         * @param componentFilter Filter added to the artifactView: Only components passing the filter will be resolved
         * @param dependencyFilter Filter added to the [ResolvableDependencies]: Only dependencies passing the filter will be resolved
         */
        @ExternalKotlinTargetApi
        class ResolvableConfiguration @JvmOverloads constructor(
            internal val configurationSelector: (KotlinSourceSet) -> Configuration?,
            override val setupArtifactViewAttributes: AttributeContainer.(sourceSet: KotlinSourceSet) -> Unit = {},
            override val componentFilter: ((ComponentIdentifier) -> Boolean)? = null,
            override val dependencyFilter: ((Dependency) -> Boolean)? = null,
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
         * @param dependencyFilter Filter added to the [ResolvableDependencies]: Only dependencies passing the filter will be resolved
         */
        @ExternalKotlinTargetApi
        class PlatformLikeSourceSet @JvmOverloads constructor(
            internal val setupPlatformResolutionAttributes: AttributeContainer.(sourceSet: KotlinSourceSet) -> Unit,
            override val setupArtifactViewAttributes: AttributeContainer.(sourceSet: KotlinSourceSet) -> Unit = {},
            override val componentFilter: ((ComponentIdentifier) -> Boolean)? = null,
            internal val dependencySubstitution: ((DependencySubstitutions) -> Unit)? = null,
            override val dependencyFilter: ((Dependency) -> Boolean)? = null,
        ) : ArtifactResolutionStrategy()
    }

    override fun resolve(sourceSet: KotlinSourceSet): Set<IdeaKotlinDependency> {
        val artifacts = artifactResolutionStrategy.createArtifactView(sourceSet.internal)?.artifacts ?: return emptySet()

        val unresolvedDependencies = artifacts.failures
            .onEach { reason -> sourceSet.project.logger.info("Failed to resolve platform dependency on ${sourceSet.name}", reason) }
            .mapNotNull { reason ->
                val selector = (reason as? ModuleVersionResolveException)?.selector

                /* We failed to resolve a library module (e.g., from a remote repository) */
                if (selector is ModuleComponentSelector)
                    return@mapNotNull IdeaKotlinUnresolvedBinaryDependency(
                        coordinates = IdeaKotlinBinaryCoordinates(selector.group, selector.module, selector.version, null),
                        cause = reason.message?.takeIf { it.isNotBlank() },
                        extras = mutableExtrasOf()
                    )

                /*
                We failed to resolve the same project as the SourceSet was declared to in a
                'PlatformLikeSourceSet' mode: We ignore this error:
                https://youtrack.jetbrains.com/issue/KT-59020/
                It seems like 'detachedConfiguration' causes an issue resolving to its project.
                */
                if (selector is ProjectComponentSelector &&
                    selector.projectPath == sourceSet.project.path &&
                    artifactResolutionStrategy is ArtifactResolutionStrategy.PlatformLikeSourceSet
                ) {
                    return@mapNotNull null
                }

                /* Can't figure out the dependency here :( */
                IdeaKotlinUnresolvedBinaryDependency(
                    coordinates = null, cause = reason.message?.takeIf { it.isNotBlank() }, extras = mutableExtrasOf()
                )
            }.toSet()

        val resolvedDependencies = artifacts.artifacts.mapNotNull { artifact ->
            when (val componentId = artifact.id.componentIdentifier) {
                is ProjectComponentIdentifier -> {
                    IdeaKotlinProjectArtifactDependency(
                        type = IdeaKotlinSourceDependency.Type.Regular,
                        coordinates = IdeaKotlinProjectCoordinates(componentId)
                    ).apply {
                        artifactsClasspath.add(artifact.file)
                    }
                }

                is ModuleComponentIdentifier -> {
                    IdeaKotlinResolvedBinaryDependency(
                        coordinates = IdeaKotlinBinaryCoordinates(componentId, artifact.variant.capabilities, artifact.variant.attributes),
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
                            sourceSetName = null,
                            capabilities = artifact.variant.capabilities.map(::IdeaKotlinBinaryCapability).toSet(),
                            attributes = IdeaKotlinBinaryAttributes(artifact.variant.attributes)
                        ), classpath = IdeaKotlinClasspath(artifact.file)
                    )
                }

                is OpaqueComponentArtifactIdentifier -> {
                    /* Files within the build directory still require a custom resolver */
                    if (
                        artifact.file.absoluteFile.startsWith(
                            sourceSet.project.layout.buildDirectory.get().asFile.absoluteFile
                        )
                    ) {
                        return@mapNotNull null
                    }

                    IdeaKotlinResolvedBinaryDependency(
                        binaryType = binaryType, coordinates = IdeaKotlinBinaryCoordinates(
                            group = "<file>",
                            module = artifact.file.relativeOrAbsolute(sourceSet.project.rootDir),
                            version = null,
                            sourceSetName = null
                        ),
                        classpath = IdeaKotlinClasspath(componentId.file)
                    ).also { dependency ->
                        dependency.isOpaqueFileDependency = true
                    }
                }

                else -> {
                    logger.warn("Unhandled componentId: ${componentId.javaClass}")
                    null
                }
            }?.also { dependency ->
                dependency.gradleArtifact = artifact
            }
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

        return createArtifactViewFromConfiguration(sourceSet, compilation.internal.configurations.compileDependencyConfiguration)
    }

    private fun ArtifactResolutionStrategy.ResolvableConfiguration.createArtifactView(sourceSet: InternalKotlinSourceSet): ArtifactView? {
        val configuration = configurationSelector(sourceSet) ?: return null
        return createArtifactViewFromConfiguration(sourceSet, configuration)
    }

    private fun ArtifactResolutionStrategy.PlatformLikeSourceSet.createArtifactView(sourceSet: InternalKotlinSourceSet): ArtifactView? {
        if (sourceSet !is DefaultKotlinSourceSet) return null
        val project = sourceSet.project

        val platformLikeCompileDependenciesConfiguration = project.configurations.detachedResolvable()
        platformLikeCompileDependenciesConfiguration.attributes.setupPlatformResolutionAttributes(sourceSet)
        platformLikeCompileDependenciesConfiguration.dependencies.addAll(sourceSet.resolvableMetadataConfiguration.allDependencies)

        if (dependencySubstitution != null) {
            platformLikeCompileDependenciesConfiguration.resolutionStrategy.dependencySubstitution(dependencySubstitution)
        }

        return createArtifactViewFromConfiguration(sourceSet, platformLikeCompileDependenciesConfiguration)
    }

    private fun ArtifactResolutionStrategy.createArtifactViewFromConfiguration(
        sourceSet: KotlinSourceSet, configuration: Configuration,
    ): ArtifactView = (if (dependencyFilter != null) configuration.copyRecursive(dependencyFilter) else configuration).incoming
        .artifactView { view ->
            view.isLenient = true
            view.attributes.setupArtifactViewAttributes(sourceSet)
            if (componentFilter != null) {
                view.componentFilter(componentFilter)
            }
        }

    private companion object {
        val logger: Logger = Logging.getLogger(IdeBinaryDependencyResolver::class.java)
    }
}
