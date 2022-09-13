/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.gradle.api.Project
import org.gradle.api.artifacts.ArtifactCollection
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ResolveException
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
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
class ResolvedDependencyGraph
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

    fun dependencyArtifactsOrNull(dependency: ResolvedDependencyResult): List<ResolvedArtifactResult>? = try {
        dependencyArtifacts(dependency)
    } catch (_: ResolveException) {
        null
    }

    fun moduleArtifacts(dependency: ResolvedDependencyResult): List<ResolvedArtifactResult> {
        val componentId = dependency.resolvedVariant.owner
        return artifactsByComponentId[componentId] ?: emptyList()
    }

    /**
     * [ResolvedVariantResult.getExternalVariant] is available in Gradle API since 6.8
     * For lower gradle versions this variant can be calculated Heuristically
     *
     * According to Gradle Module Specification
     * https://github.com/gradle/gradle/blob/master/subprojects/docs/src/docs/design/gradle-module-metadata-latest-specification.md
     *
     * Variants with `available-at` cannot have neither `files` nor `dependencies` so this heuristic is based on those facts
     *
     * If original [ResolvedDependencyResult] doesn't have a related artifact available by its Component ID
     * AND it contains only single dependency which is in fact a component pointed through `available-at`
     */
    fun ResolvedDependencyResult.findExternalVariantHeuristically(): ResolvedDependencyResult? {
        val componentId = selected.id

        if (componentId in artifactsByComponentId) return null
        val dependency = selected.dependencies.singleOrNull() ?: return null
        if (dependency !is ResolvedDependencyResult) return null

        return dependency
    }

    override fun toString(): String = "ResolvedDependencyGraph(configuration='$name')"
}

val ResolvedDependencyGraph.allResolvedDependencies get() = allDependencies.filterIsInstance<ResolvedDependencyResult>()