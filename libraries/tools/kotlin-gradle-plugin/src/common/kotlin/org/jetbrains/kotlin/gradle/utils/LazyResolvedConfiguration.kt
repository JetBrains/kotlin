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
internal class LazyResolvedConfiguration private constructor(
    private val resolvedComponentsRootProvider: Lazy<ResolvedComponentResult>,
    private val artifactCollection: ArtifactCollection,
    private val configurationName: String,
) {

    /**
     * Creates [LazyResolvedConfiguration] from given [configuration].
     * The underlying ArtifactView can be configured with [configureArtifactView] or [configureArtifactViewAttributes]
     */
    constructor(
        configuration: Configuration,
        configureArtifactView: ArtifactView.ViewConfiguration.() -> Unit = {},
        configureArtifactViewAttributes: (AttributeContainer) -> Unit = {},
    ) : this(
        // Calling resolutionResult doesn't actually trigger resolution. But accessing its root ResolvedComponentResult
        // via ResolutionResult::root does. ResolutionResult can't be serialised for Configuration Cache
        // but ResolvedComponentResult can. Wrapping it in `lazy` makes it resolve upon serialisation.
        resolvedComponentsRootProvider = configuration.incoming.resolutionResult.let { rr -> lazy { rr.root } },
        artifactCollection = configuration.lazyArtifactCollection {
            attributes(configureArtifactViewAttributes)
            configureArtifactView()
        },
        configurationName = configuration.name
    )

    val files: FileCollection get() = artifactCollection.artifactFiles

    val root by resolvedComponentsRootProvider

    val resolvedArtifacts: Set<ResolvedArtifactResult> get() = artifactCollection.artifacts

    val resolutionFailures: Collection<Throwable> get() = artifactCollection.failures

    private val artifactsByComponentId by TransientLazy { resolvedArtifacts.groupBy { it.id.componentIdentifier } }

    val allDependencies: Set<DependencyResult> by TransientLazy {
        root.dependencies.withClosure<DependencyResult> {
            if (it is ResolvedDependencyResult) it.selected.dependencies
            else emptyList()
        }
    }

    internal val allResolvedDependencies: Set<ResolvedDependencyResult> by TransientLazy {
        allDependencies.filterIsInstance<ResolvedDependencyResult>().toSet()
    }

    fun getArtifacts(dependency: ResolvedDependencyResult): List<ResolvedArtifactResult> {
        val componentId = dependency.resolvedVariant.lastExternalVariantOrSelf().owner
        return artifactsByComponentId[componentId] ?: emptyList()
    }

    fun getArtifacts(component: ResolvedComponentResult): List<ResolvedArtifactResult> {
        val componentIds = component.variants.map { it.lastExternalVariantOrSelf().owner }
        return componentIds.flatMap { artifactsByComponentId[it].orEmpty() }
    }

    fun getArtifacts(componentId: ComponentIdentifier): List<ResolvedArtifactResult> = artifactsByComponentId[componentId].orEmpty()

    override fun toString(): String = "LazyResolvedConfiguration(configuration='$configurationName')"
}

private fun Configuration.lazyArtifactCollection(configureArtifactView: ArtifactView.ViewConfiguration.() -> Unit): ArtifactCollection =
    incoming.artifactView { view ->
        view.isLenient = true
        view.configureArtifactView()
    }.artifacts

internal tailrec fun ResolvedVariantResult.lastExternalVariantOrSelf(): ResolvedVariantResult {
    return if (externalVariant.isPresent) externalVariant.get().lastExternalVariantOrSelf() else this
}

/**
 * Same as [LazyResolvedConfiguration.getArtifacts] except it returns null for cases when dependency is resolved
 * but artifact is not available. For example when host-specific part of the library is not yet published
 */
internal fun LazyResolvedConfiguration.dependencyArtifactsOrNull(dependency: ResolvedDependencyResult): List<ResolvedArtifactResult>? =
    try {
        getArtifacts(dependency)
    } catch (_: ResolveException) {
        null
    }