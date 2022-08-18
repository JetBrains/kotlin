/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.logging.Logger
import org.jetbrains.kotlin.library.resolveSingleFileKlib
import org.jetbrains.kotlin.library.uniqueName
import java.io.File
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

internal fun getCacheDirectory(
    rootCacheDirectory: File,
    dependency: ResolvedDependencyResult,
    artifact: ResolvedArtifactResult?,
    resolvedDependencyGraph: ResolvedDependencyGraph,
    partialLinkage: Boolean
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
            resolveSingleFileKlib(org.jetbrains.kotlin.konan.file.File(it.absolutePath))
        }
        ?.uniqueName

    val cacheDirectory = if (uniqueName != null) {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(uniqueName.toByteArray(StandardCharsets.UTF_8)).toHexString()
        versionCacheDirectory.resolve(hash)
    } else versionCacheDirectory

    return File(cacheDirectory, computeDependenciesHash(dependency, resolvedDependencyGraph, partialLinkage))
}

internal fun ByteArray.toHexString() = joinToString("") { (0xFF and it.toInt()).toString(16).padStart(2, '0') }

private fun computeDependenciesHash(dependency: ResolvedDependencyResult, resolvedDependencyGraph: ResolvedDependencyGraph, partialLinkage: Boolean): String {
    val hashedValue = buildString {
        if (partialLinkage) append("#__PL__#")

        (listOf(dependency) + getAllDependencies(dependency))
            .flatMap { resolvedDependencyGraph.dependencyArtifacts(it) }
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
    resolvedDependencyGraph: ResolvedDependencyGraph,
    considerArtifact: Boolean,
    partialLinkage: Boolean
): List<File>? {
    return getAllDependencies(dependency)
        .flatMap { childDependency ->
            resolvedDependencyGraph.dependencyArtifacts(childDependency).map {
                if (libraryFilter(it)) {
                    val cacheDirectory = getCacheDirectory(
                        rootCacheDirectory = rootCacheDirectory,
                        dependency = childDependency,
                        artifact = if (considerArtifact) it else null,
                        resolvedDependencyGraph = resolvedDependencyGraph,
                        partialLinkage = partialLinkage
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

internal class GradleLoggerAdapter(private val gradleLogger: Logger) : org.jetbrains.kotlin.util.Logger {
    override fun log(message: String) = gradleLogger.info(message)
    override fun warning(message: String) = gradleLogger.warn(message)
    override fun error(message: String) = kotlin.error(message)
    override fun fatal(message: String): Nothing = kotlin.error(message)
}

private fun libraryFilter(artifact: ResolvedArtifactResult): Boolean = artifact.file.absolutePath.endsWith(".klib")
