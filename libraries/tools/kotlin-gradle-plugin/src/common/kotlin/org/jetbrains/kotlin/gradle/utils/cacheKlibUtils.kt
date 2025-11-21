/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.jetbrains.kotlin.backend.common.linkage.partial.PartialLinkageMode
import org.jetbrains.kotlin.library.uniqueName
import org.slf4j.Logger
import java.io.File
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

internal fun getCacheDirectory(
    rootCacheDirectory: File,
    dependency: ResolvedDependencyResult,
    artifact: ResolvedArtifactResult?,
    resolvedConfiguration: LazyResolvedConfigurationWithArtifacts,
    partialLinkageMode: String,
    logger: Logger,
): File {
    val moduleCacheDirectory = File(rootCacheDirectory, dependency.selected.moduleVersion?.name ?: "undefined")
    val versionCacheDirectory = File(moduleCacheDirectory, dependency.selected.moduleVersion?.version ?: "undefined")
    val uniqueName = artifact
        ?.let {
            if (libraryFilter(it))
                it.file
            else
                null
        }
        ?.let {
            loadSingleKlib(it, logger, reportProblemsAtInfoLevel = true)
        }
        ?.uniqueName

    val cacheDirectory = if (uniqueName != null) {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(uniqueName.toByteArray(StandardCharsets.UTF_8)).toHexString()
        versionCacheDirectory.resolve(hash)
    } else versionCacheDirectory

    return File(cacheDirectory, computeDependenciesHash(dependency, resolvedConfiguration, partialLinkageMode))
}

internal fun ByteArray.toHexString() = joinToString("") { (0xFF and it.toInt()).toString(16).padStart(2, '0') }

private fun computeDependenciesHash(
    dependency: ResolvedDependencyResult,
    resolvedConfiguration: LazyResolvedConfigurationWithArtifacts,
    partialLinkageMode: String
): String {
    val hashedValue = buildString {
        if (PartialLinkageMode.resolveMode(partialLinkageMode)?.isEnabled == true)
            append("#__PL__#")

        (listOf(dependency) + getAllDependencies(dependency))
            .flatMap { resolvedConfiguration.getArtifacts(it) }
            .map { it.file.absolutePath }
            .distinct()
            .sortedBy { it }
            .joinTo(this, separator = "|")
    }

    val digest = MessageDigest.getInstance("SHA-256")
    val hash = digest.digest(hashedValue.toByteArray(StandardCharsets.UTF_8))
    return hash.toHexString()
}

internal fun getDependenciesCacheDirectories(
    rootCacheDirectory: File,
    dependency: ResolvedDependencyResult,
    resolvedConfiguration: LazyResolvedConfigurationWithArtifacts,
    considerArtifact: Boolean,
    partialLinkageMode: String,
    logger: Logger,
): List<File>? {
    return getAllDependencies(dependency)
        .flatMap { childDependency ->
            resolvedConfiguration.getArtifacts(childDependency).map {
                if (libraryFilter(it)) {
                    val cacheDirectory = getCacheDirectory(
                        rootCacheDirectory = rootCacheDirectory,
                        dependency = childDependency,
                        artifact = if (considerArtifact) it else null,
                        resolvedConfiguration = resolvedConfiguration,
                        partialLinkageMode = partialLinkageMode,
                        logger = logger,
                    )
                    if (!cacheDirectory.exists()) return null
                    cacheDirectory
                } else {
                    null
                }
            }
        }
        .filterNotNull()
        .filter { it.exists() }
}

internal fun getAllDependencies(dependency: ResolvedDependencyResult): Set<ResolvedDependencyResult> {
    val allDependencies = mutableSetOf<ResolvedDependencyResult>()

    fun traverseAllDependencies(dependency: ResolvedDependencyResult) {
        if (dependency in allDependencies)
            return
        allDependencies.add(dependency)
        dependency.selected.dependencies.filterIsInstance<ResolvedDependencyResult>().forEach { traverseAllDependencies(it) }
    }

    dependency.selected.dependencies.filterIsInstance<ResolvedDependencyResult>().forEach { traverseAllDependencies(it) }
    return allDependencies
}

private fun libraryFilter(artifact: ResolvedArtifactResult): Boolean = artifact.file.absolutePath.endsWith(".klib")
