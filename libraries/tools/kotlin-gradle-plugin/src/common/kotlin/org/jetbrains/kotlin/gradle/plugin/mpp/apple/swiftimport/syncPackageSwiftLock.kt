/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.utils.contentEquals

import java.io.File
import javax.inject.Inject

@DisableCachingByDefault(because = "KT-84827 - SwiftPM import doesn't support caching yet")
internal abstract class SyncPackageResolvedTask : DefaultTask() {

    @get:Optional
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceFile: RegularFileProperty

    @get:OutputFile
    abstract val destinationFile: RegularFileProperty

    @get:Inject
    abstract val fs: FileSystemOperations

    @TaskAction
    fun sync() {
        if (!sourceFile.isPresent) return

        val src = sourceFile.get().asFile
        val dest = destinationFile.get().asFile

        if (!src.exists()) {
            if (dest.exists()) {
                dest.delete()
            }
            return
        }

        if (hasSameContent(src, dest)) return

        if (!dest.parentFile.exists()) dest.parentFile.mkdirs()

        copySwiftLockFile(fs, src, dest)
    }

    companion object {
        const val TASK_NAME = "syncPersistedPackageResolved"
        const val SYNC_SYNTHETIC_PACKAGE_RESOLVED_TO_PERSISTED_TASK_NAME = "syncSyntheticPackageResolvedToPersisted"
        const val SYNC_PERSISTED_PACKAGE_RESOLVED_TO_SYNTHETIC_TASK_NAME = "syncPersistedPackageResolvedToSynthetic"
    }
}


private fun copySwiftLockFile(
    fs: FileSystemOperations,
    src: File,
    dest: File,
) {
    fs.copy { spec ->
        spec.from(src)
        spec.into(dest.parentFile)
        spec.rename { dest.name }
    }
}

private fun hasSameContent(dest: File, src: File): Boolean = dest.exists() && src.exists() && contentEquals(src, dest)

/**
 * One project's contribution to a shared lock bucket.
 *
 * @param projectPath The Gradle project path (e.g., ":shared", ":sub")
 * @param directMetadata This project's direct SwiftPM import metadata
 * @param transitiveDependencies Transitive SwiftPM dependencies from this project
 */
internal data class SwiftPMLockTaskContribution(
    val projectPath: String,
    val directMetadata: Provider<SwiftPMImportMetadata>,
    val transitiveDependencies: Provider<TransitiveSwiftPMDependencies>,
)

/**
 * Shared build service that aggregates SwiftPM metadata from all contributing projects.
 * All umbrella generate/fetch tasks run, but only one actually produces output (via build service flags).
 */
internal abstract class SwiftPMLockTaskAggregationBuildService : BuildService<BuildServiceParameters.None> {

    private val stateLock = Any()
    private val contributionsByIdentifier = mutableMapOf<String, MutableList<SwiftPMLockTaskContribution>>()
    private val serializeTaskByProjects = mutableMapOf<String, MutableList<String>>()

    private val claimedGenerateTaskByIdentifier = mutableMapOf<String, String>()
    private val claimedFetchTaskByIdentifier = mutableMapOf<String, String>()

    /** Registers a project's contribution for [identifier]. */
    fun contribute(
        identifier: String,
        serializeSwiftPMDependenciesMetadataTaskName: String? = null,
        contribution: SwiftPMLockTaskContribution,
    ) {
        synchronized(stateLock) {
            // Add it to the current identifier bucket
            contributionsByIdentifier
                .getOrPut(identifier) { mutableListOf() }
                .add(contribution)

            if (serializeSwiftPMDependenciesMetadataTaskName != null) {
                serializeTaskByProjects.getOrPut(identifier) { mutableListOf() }.add(
                    serializeSwiftPMDependenciesMetadataTaskName
                )
            }
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


    fun buildAggregatedResultDependencies(identifier: String): TransitiveSwiftPMDependencies {
        val contributions = synchronized(stateLock) {
            contributionsByIdentifier[identifier].orEmpty().toList()
        }

        if (contributions.isEmpty()) return TransitiveSwiftPMDependencies(emptyMap())

        val merged = linkedMapOf<SwiftPMDependencyIdentifier, SwiftPMImportMetadata>()

        contributions
            .sortedBy { it.projectPath }
            .forEach { contribution ->
                val selfIdentifier = SwiftPMDependencyIdentifier(
                    contribution.projectPath.replace(":", "_")
                )

                merged[selfIdentifier] = contribution.directMetadata.get()

                contribution.transitiveDependencies.get().metadataByDependencyIdentifier
                    .entries
                    .sortedBy { it.key.identifier }
                    .forEach { (dependencyIdentifier, metadata) ->
                        merged.putIfAbsent(dependencyIdentifier, metadata)
                    }
            }

        val deterministic = linkedMapOf<SwiftPMDependencyIdentifier, SwiftPMImportMetadata>()
        merged.entries
            .sortedBy { it.key.identifier }
            .forEach { (dependencyIdentifier, metadata) ->
                deterministic[dependencyIdentifier] = metadata
            }

        return TransitiveSwiftPMDependencies(deterministic)
    }

    fun getContributorProjectsSerializeTasks(identifier: String): List<String> {
        return synchronized(stateLock) {
            serializeTaskByProjects.getOrDefault(identifier, emptyList())
        }
    }


    companion object {
        private const val SERVICE_NAME = "swiftPmLockTaskAggregationService"

        /** Registers the shared service once per build. */
        fun registerIfAbsent(project: Project): Provider<SwiftPMLockTaskAggregationBuildService> =
            project.gradle.sharedServices.registerIfAbsent(
                SERVICE_NAME,
                SwiftPMLockTaskAggregationBuildService::class.java
            ) { spec ->
                spec.maxParallelUsages.set(1)
            }
    }
}



