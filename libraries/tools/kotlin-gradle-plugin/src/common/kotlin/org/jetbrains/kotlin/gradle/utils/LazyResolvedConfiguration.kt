/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.gradle.api.artifacts.ArtifactCollection
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ResolveException
import org.gradle.api.artifacts.result.*
import org.gradle.api.file.FileCollection

/**
 * Represents a Gradle Configuration that was resolved after configuration time.
 * But still can be accessed during Configuration time, triggering configuration resolution
 *
 * Serializable to configuration cache. So it can be stored in task state and be accessed during execution time.
 *
 * Has similar API as non-configuration cache friendly Gradle's [ResolvedConfiguration]
 */
internal class LazyResolvedConfiguration
private constructor(
    private val resolvedComponentsRootProvider: Lazy<ResolvedComponentResult>,
    private val artifactCollection: ArtifactCollection,
    private val configurationName: String,
) {
    constructor(configuration: Configuration) : this(
        // Calling resolutionResult doesn't actually trigger resolution. But accessing its root ResolvedComponentResult
        // via ResolutionResult::root does. ResolutionResult can't be serialised for Configuration Cache
        // but ResolvedComponentResult can. Wrapping it in `lazy` makes it resolve upon serialisation.
        resolvedComponentsRootProvider = configuration.incoming.resolutionResult.let { rr -> lazy { rr.root } },
        artifactCollection = configuration.incoming.artifacts, // lazy ArtifactCollection
        configurationName = configuration.name
    )

    val root get() = resolvedComponentsRootProvider.value
    val files: FileCollection get() = artifactCollection.artifactFiles
    val artifacts get() = artifactCollection.artifacts

    private val artifactsByComponentId by TransientLazy { artifacts.groupBy { it.id.componentIdentifier } }

    val allDependencies: List<DependencyResult> get() {
        fun DependencyResult.allDependenciesRecursive(): List<DependencyResult> =
            if (this is ResolvedDependencyResult) {
                listOf(this) + selected.dependencies.flatMap { it.allDependenciesRecursive() }
            } else {
                listOf(this)
            }

        return root.dependencies.flatMap { it.allDependenciesRecursive() }
    }

    /**
     * Returns artifacts of a given dependency walking through external variants (the ones that referenced in `available-at`)
     * For example given a MPP library:
     *        lib
     *      /     \
     *  lib-jvm  lib-iosX64
     *
     *  And some requested dependency on `lib` in `jvmMain` source set. In this case it should be resolved to `lib-jvm`
     *  so requesting [dependencyArtifacts] for `lib` should actually return a jvm artifact from `lib-jvm`
     *
     *  If you need to avoid such behavior see [moduleArtifacts]
     */
    fun dependencyArtifacts(dependency: ResolvedDependencyResult): List<ResolvedArtifactResult> {
        fun ResolvedVariantResult.findNonExternalVariant(): ResolvedVariantResult =
            if (externalVariant.isPresent) {
                externalVariant.get().findNonExternalVariant()
            } else {
                this
            }

        val componentId = dependency.resolvedVariant.findNonExternalVariant().owner
        return artifactsByComponentId[componentId] ?: emptyList()
    }

    /**
     * Returns own artifacts of a given dependency.
     * Acts in the same way as [org.gradle.api.artifacts.ResolvedDependency.getModuleArtifacts].
     *
     * To get artifacts from External Variants (aka `available-at`) use [dependencyArtifacts]
     */
    fun moduleArtifacts(dependency: ResolvedDependencyResult): List<ResolvedArtifactResult> {
        val componentId = dependency.resolvedVariant.owner
        return artifactsByComponentId[componentId] ?: emptyList()
    }

    override fun toString(): String = "ResolvedDependencyGraph(configuration='$configurationName')"
}

internal val LazyResolvedConfiguration.allResolvedDependencies: Set<ResolvedDependencyResult> get() = allDependencies
    .filterIsInstance<ResolvedDependencyResult>()
    .toSet()

/**
 * Same as [LazyResolvedConfiguration.dependencyArtifacts] except it returns null for cases when dependency is resolved
 * but artifact is not available. For example when host-specific part of the library is not yet published
 */
internal fun LazyResolvedConfiguration.dependencyArtifactsOrNull(dependency: ResolvedDependencyResult): List<ResolvedArtifactResult>? = try {
    dependencyArtifacts(dependency)
} catch (_: ResolveException) {
    null
}