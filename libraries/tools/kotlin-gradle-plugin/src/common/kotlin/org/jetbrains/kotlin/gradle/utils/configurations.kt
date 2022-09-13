/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.gradle.api.artifacts.ArtifactCollection
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ResolveException
import org.gradle.api.artifacts.result.DependencyResult
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.artifacts.result.ResolvedVariantResult
import org.gradle.api.file.FileCollection

const val COMPILE_ONLY = "compileOnly"
const val COMPILE = "compile"
const val IMPLEMENTATION = "implementation"
const val API = "api"
const val RUNTIME_ONLY = "runtimeOnly"
const val RUNTIME = "runtime"
internal const val INTRANSITIVE = "intransitive"

/**
 * Gradle Configuration Cache-friendly representation of resolved Configuration
 */
internal class ResolvedDependencyGraph
private constructor(
    private val resolvedComponentsRootProvider: Lazy<ResolvedComponentResult>,
    private val artifactCollection: ArtifactCollection,
    val name: String
) {
    constructor(configuration: Configuration) : this(
        // Calling resolutionResult doesn't actually trigger resolution. But accessing its root ResolvedComponentResult
        // via ResolutionResult::root does. ResolutionResult can't be serialised for Configuration Cache
        // but ResolvedComponentResult can. Wrapping it in `lazy` makes it resolve upon serialisation.
        resolvedComponentsRootProvider = configuration.incoming.resolutionResult.let { rr -> lazy { rr.root } },
        artifactCollection = configuration.incoming.artifacts, // lazy ArtifactCollection
        name = configuration.name
    )

    val root: ResolvedComponentResult get() = resolvedComponentsRootProvider.value
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
     * Same as [dependencyArtifacts] except it returns null for cases when dependency is resolved
     * but artifact is not available. For example when host-specific part of the library is not yet published
     */
    fun dependencyArtifactsOrNull(dependency: ResolvedDependencyResult): List<ResolvedArtifactResult>? = try {
        dependencyArtifacts(dependency)
    } catch (_: ResolveException) {
        null
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

    override fun toString(): String = "ResolvedDependencyGraph(configuration='$name')"
}

internal val ResolvedDependencyGraph.allResolvedDependencies get() = allDependencies.filterIsInstance<ResolvedDependencyResult>()