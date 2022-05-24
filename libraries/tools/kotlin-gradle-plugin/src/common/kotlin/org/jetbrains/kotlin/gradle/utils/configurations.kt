/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ResolvedConfiguration
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.artifacts.result.ResolvedComponentResult
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
 * Gradle Configuration Cache-friendly holder of resolved Configuration
 */
class FilesWithResolvedDependencyGraph
private constructor (
    val files: FileCollection,
    val graphRootProvider: Provider<ResolvedComponentResult>
) {
    val graphRoot get() = graphRootProvider.get()

    companion object {
        operator fun invoke(project: Project, configuration: Configuration) = FilesWithResolvedDependencyGraph(
            files = project.files(configuration),
            graphRootProvider = configuration.incoming.resolutionResult.let { rr -> project.provider { rr.root } }
        )
    }
}

/**
 * Configuration Cache-friendly alternative of [ResolvedConfiguration.getFirstLevelModuleDependencies]
 */
internal val ResolvedComponentResult.firstLevelModuleDependencies: Set<ResolvedDependency> get() =
    dependencies.filterIsInstance<ResolvedDependency>().toSet()