/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.jetbrains.kotlin.gradle.utils.registerClassLoaderScopedBuildService

/**
 * One project's contribution to a shared lock bucket.
 *
 * @param projectPath The Gradle project path (e.g., ":shared", ":sub")
 * @param directMetadata This project's direct SwiftPM import metadata
 * @param transitiveDependencies Transitive SwiftPM dependencies from this project
 */


/**
 * Shared build service that aggregates SwiftPM metadata from all contributing projects.
 * All umbrella generate/fetch tasks run, but only one actually produces output (via build service flags).
 */
internal abstract class SwiftPMLockTaskAggregationBuildService : BuildService<BuildServiceParameters.None> {

    private val stateLock = Any()
    private val projectPathsByIdentifier = mutableMapOf<String, MutableList<String>>()

    private val claimedGenerateTaskByIdentifier = mutableMapOf<String, String>()
    private val claimedFetchTaskByIdentifier = mutableMapOf<String, String>()

    /** Registers a project's contribution for [identifier]. */
    fun contribute(
        identifier: String,
        projectPathContribution: String,
    ) {
        synchronized(stateLock) {
            // Add it to the current identifier bucket
            projectPathsByIdentifier
                .getOrPut(identifier) { mutableListOf() }
                .add(projectPathContribution)
        }
    }

    fun claimGenerateTask(identifier: String, taskName: String): Boolean {
        synchronized(stateLock) {
            return taskName == claimedGenerateTaskByIdentifier.getOrPut(identifier) { taskName }
        }
    }

    fun claimFetchTask(identifier: String, taskName: String): Boolean {
        synchronized(stateLock) {
            return taskName == claimedFetchTaskByIdentifier.getOrPut(identifier) { taskName }
        }
    }

    fun getClaimedGenerateTask(identifier: String): String? = claimedGenerateTaskByIdentifier[identifier]
    fun getClaimedFetchTask(identifier: String): String? = claimedFetchTaskByIdentifier[identifier]

    fun buildAggregatedResultDependencies(identifier: String): List<String> {
        val projectPaths = synchronized(stateLock) {
            projectPathsByIdentifier[identifier].orEmpty().toList()
        }

        return projectPaths
    }

    companion object {
        /** Registers the shared service once per build. */
        fun registerIfAbsent(project: Project): Provider<SwiftPMLockTaskAggregationBuildService> =
            project.gradle.registerClassLoaderScopedBuildService(SwiftPMLockTaskAggregationBuildService::class)
    }
}
