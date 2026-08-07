/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.gradle.api.artifacts.ArtifactCollection
import org.gradle.api.artifacts.ArtifactView
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ResolvedConfiguration
import org.gradle.api.artifacts.component.ComponentSelector
import org.gradle.api.artifacts.result.*
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.cache.KotlinGradleTaskExecutionCache
import org.jetbrains.kotlin.gradle.plugin.internal.BuildIdentifierAccessor
import org.jetbrains.kotlin.gradle.plugin.mpp.KmpModuleIdentifier
import org.jetbrains.kotlin.tooling.core.withClosure

/**
 * Represents a Gradle Configuration that was resolved after configuration time.
 * But still can be accessed during Configuration time, triggering configuration resolution
 *
 * Serializable to configuration cache. So it can be stored in task state and be accessed during execution time.
 *
 * Has similar API as non-configuration cache friendly Gradle's [ResolvedConfiguration]
 */
internal class LazyResolvedConfigurationComponent private constructor(
    private val resolvedComponentsRootProvider: Lazy<ResolvedComponentResult>,
    val configurationName: String,
) {

    /**
     * Creates [LazyResolvedConfigurationWithArtifacts] from given [configuration].
     * The underlying ArtifactView can be configured with [configureArtifactView] or [configureArtifactViewAttributes]
     */
    constructor(
        configuration: Configuration,
    ) : this(
        // Calling resolutionResult doesn't actually trigger resolution. But accessing its root ResolvedComponentResult
        // via ResolutionResult::root does. ResolutionResult can't be serialised for Configuration Cache
        // but ResolvedComponentResult can. Wrapping it in `lazy` makes it resolve upon serialisation.
        resolvedComponentsRootProvider = configuration.incoming.resolutionResult.let { rr -> lazy { rr.root } },
        configurationName = configuration.name
    )

    val root by resolvedComponentsRootProvider

    val allDependencies: Set<DependencyResult> by TransientLazy {
        root.dependencies.withClosure<DependencyResult> {
            if (it is ResolvedDependencyResult) it.selected.dependencies
            else emptyList()
        }
    }

    internal val allResolvedDependencies: Set<ResolvedDependencyResult> by TransientLazy {
        allDependencies.filterIsInstance<ResolvedDependencyResult>().toSet()
    }

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

internal fun LazyResolvedConfigurationComponent.resolvedDependenciesByKmpModuleId(
    cache: KotlinGradleTaskExecutionCache,
    projectId: String,
    buildIdentifierAccessor: Provider<BuildIdentifierAccessor.Factory>,
): Map<KmpModuleIdentifier, Set<ResolvedDependencyResult>> =
    cache.getOrCompute("$projectId/$configurationName/resolvedDependenciesByKmpModuleId") {
        groupByNotNullToSet(
            keySelector = { KmpModuleIdentifier.from(it.from, buildIdentifierAccessor) },
            valueTransform = { it as? ResolvedDependencyResult },
        )
    }

internal fun LazyResolvedConfigurationComponent.resolvedDependenciesByRequested(
    cache: KotlinGradleTaskExecutionCache,
    projectId: String,
): Map<ComponentSelector, Set<ResolvedDependencyResult>> =
    cache.getOrCompute("$projectId/$configurationName/resolvedDependenciesByRequested") {
        groupByNotNullToSet(
            keySelector = { it.requested },
            valueTransform = { it as? ResolvedDependencyResult },
        )
    }

internal fun <K, V> LazyResolvedConfigurationComponent.groupByNotNullToSet(
    keySelector: (DependencyResult) -> K?,
    valueTransform: (DependencyResult) -> V?,
): Map<K, Set<V>> {
    val computed = mutableMapOf<K, MutableSet<V>>()
    for (dependency in allDependencies) {
        val key = keySelector(dependency) ?: continue

        val value = valueTransform(dependency)
        if (value != null) {
            computed.getOrPut(key) { mutableSetOf() } += value
        }
    }
    return computed
}
