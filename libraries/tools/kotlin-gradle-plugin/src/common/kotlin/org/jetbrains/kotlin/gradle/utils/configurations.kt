/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
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
private constructor (
    val files: FileCollection,
    private val graphRootProvider: Provider<ResolvedComponentResult>,
    private val artifactsProvider: Provider<Set<ResolvedArtifactResult>>
) {
    val root get() = graphRootProvider.get()
    val artifacts get() = artifactsProvider.get()

    val artifactsByComponentId by lazy { artifacts.groupBy { it.id.componentIdentifier } }

    fun dependencyArtifacts(dependency: ResolvedDependencyResult): List<ResolvedArtifactResult> {
        val componentId = dependency.selected.id
        return artifactsByComponentId[componentId] ?: emptyList()
    }

    companion object {
        operator fun invoke(project: Project, configuration: Configuration) = ResolvedDependencyGraph(
            files = project.files(configuration),
            graphRootProvider = configuration.incoming.resolutionResult.let { rr -> project.provider { rr.root } },
            artifactsProvider = configuration.incoming.artifacts.let { collection -> project.provider { collection.artifacts } }
        )
    }
}