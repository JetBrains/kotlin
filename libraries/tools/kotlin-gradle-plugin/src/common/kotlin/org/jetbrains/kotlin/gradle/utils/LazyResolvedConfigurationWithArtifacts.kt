/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.gradle.api.artifacts.ArtifactCollection
import org.gradle.api.artifacts.ArtifactView
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ResolveException
import org.gradle.api.artifacts.ResolvedConfiguration
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.artifacts.result.*
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.file.FileCollection
import org.jetbrains.kotlin.tooling.core.withClosure

/**
 * Represents a Gradle Configuration that was resolved after configuration time.
 * But still can be accessed during Configuration time, triggering configuration resolution
 *
 * Serializable to configuration cache. So it can be stored in task state and be accessed during execution time.
 *
 * Has similar API as non-configuration cache friendly Gradle's [ResolvedConfiguration]
 */
internal class LazyResolvedConfigurationWithArtifacts private constructor(
    val resolvedComponent: LazyResolvedConfigurationComponent,
    private val artifactCollection: ArtifactCollection,
    val configurationName: String,
) {

    /**
     * Creates [LazyResolvedConfigurationWithArtifacts] from given [configuration].
     * The underlying ArtifactView can be configured with [configureArtifactView] or [configureArtifactViewAttributes]
     */
    constructor(
        configuration: Configuration,
        configureArtifactView: ArtifactView.ViewConfiguration.() -> Unit = {},
        configureArtifactViewAttributes: (AttributeContainer) -> Unit = {},
    ) : this(
        resolvedComponent = LazyResolvedConfigurationComponent(configuration),
        artifactCollection = configuration.lazyArtifactCollection {
            attributes(configureArtifactViewAttributes)
            configureArtifactView()
        },
        configurationName = configuration.name
    )

    val files: FileCollection get() = artifactCollection.artifactFiles

    val resolvedArtifacts: Set<ResolvedArtifactResult> get() = artifactCollection.artifacts

    val resolutionFailures: Collection<Throwable> get() = artifactCollection.failures

    private val artifactsByComponentId by TransientLazy { resolvedArtifacts.groupBy { it.id.componentIdentifier } }

    fun getArtifacts(dependency: ResolvedDependencyResult): List<ResolvedArtifactResult> {
        val componentId = dependency.resolvedVariant.lastExternalVariantOrSelf().owner
        return artifactsByComponentId[componentId] ?: emptyList()
    }

    fun getArtifacts(component: ResolvedComponentResult): List<ResolvedArtifactResult> {
        val componentIds = component.variants.map { it.lastExternalVariantOrSelf().owner }
        return componentIds.flatMap { artifactsByComponentId[it].orEmpty() }
    }

    fun getArtifacts(componentId: ComponentIdentifier): List<ResolvedArtifactResult> = artifactsByComponentId[componentId].orEmpty()

    /** Copy from [LazyResolvedConfigurationComponent] for convenience */
    val root get() = resolvedComponent.root
    val allDependencies: Set<DependencyResult> get() = resolvedComponent.allDependencies
    val allResolvedDependencies: Set<ResolvedDependencyResult> get() = resolvedComponent.allResolvedDependencies

    override fun toString(): String = "LazyResolvedConfigurationWithArtifacts(configuration='$configurationName')"
}

private fun Configuration.lazyArtifactCollection(configureArtifactView: ArtifactView.ViewConfiguration.() -> Unit): ArtifactCollection =
    incoming.artifactView { view ->
        view.isLenient = true
        view.configureArtifactView()
    }.artifacts

/**
 * Same as [LazyResolvedConfigurationWithArtifacts.getArtifacts] except it returns null for cases when dependency is resolved
 * but artifact is not available. For example when host-specific part of the library is not yet published
 */
internal fun LazyResolvedConfigurationWithArtifacts.dependencyArtifactsOrNull(dependency: ResolvedDependencyResult): List<ResolvedArtifactResult>? =
    try {
        getArtifacts(dependency)
    } catch (_: ResolveException) {
        null
    }