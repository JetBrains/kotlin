/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.gradle.api.Project
import org.gradle.api.artifacts.ArtifactCollection
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.result.DependencyResult
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Provider

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
    private val artifactCollection: ArtifactCollection
) {
    constructor(configuration: Configuration) : this(
        // Calling resolutionResult doesn't actually trigger resolution. But accessing its root ResolvedComponentResult
        // via ResolutionResult::root does. ResolutionResult can't be serialised for Configuration Cache
        // but ResolvedComponentResult can. Wrapping it in `lazy` makes it resolve upon serialisation.
        resolvedComponentsRootProvider = configuration.incoming.resolutionResult.let { rr -> lazy { rr.root } },
        artifactCollection = configuration.incoming.artifacts // lazy ArtifactCollection
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

    fun dependencyArtifacts(dependency: ResolvedDependencyResult): List<ResolvedArtifactResult> {
        val componentId = dependency.selected.id
        return artifactsByComponentId[componentId] ?: emptyList()
    }
}

internal fun Configuration.markConsumable(): Configuration = apply {
    this.isCanBeConsumed = true
    this.isCanBeResolved = false
}

internal fun Configuration.markResolvable(): Configuration = apply {
    this.isCanBeConsumed = false
    this.isCanBeResolved = true
}
