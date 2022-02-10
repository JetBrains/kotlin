/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.logging.Logger
import org.jetbrains.kotlin.library.resolveSingleFileKlib
import org.jetbrains.kotlin.library.uniqueName
import java.io.File
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

fun getCacheDirectory(
    rootCacheDirectory: File,
    dependency: ResolvedDependency,
    artifact: ResolvedArtifact? = null,
    libraryFilter: (ResolvedArtifact) -> Boolean = { it.file.absolutePath.endsWith(".klib") }
): File {
    val moduleCacheDirectory = File(rootCacheDirectory, dependency.moduleName)
    val versionCacheDirectory = File(moduleCacheDirectory, dependency.moduleVersion)
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

    return File(cacheDirectory, computeDependenciesHash(dependency))
}

internal fun ByteArray.toHexString() = joinToString("") { (0xFF and it.toInt()).toString(16).padStart(2, '0') }

private fun computeDependenciesHash(dependency: ResolvedDependency): String {
    val allArtifactsPaths =
        (dependency.moduleArtifacts + getAllDependencies(dependency).flatMap { it.moduleArtifacts })
            .map { it.file.absolutePath }
            .distinct()
            .sortedBy { it }
            .joinToString("|") { it }
    val digest = MessageDigest.getInstance("SHA-256")
    val hash = digest.digest(allArtifactsPaths.toByteArray(StandardCharsets.UTF_8))
    return hash.toHexString()
}

fun getDependenciesCacheDirectories(
    rootCacheDirectory: File,
    dependency: ResolvedDependency,
    libraryFilter: (ResolvedArtifact) -> Boolean = { it.file.absolutePath.endsWith(".klib") },
    considerArtifact: Boolean = false
): List<File>? {
    return getAllDependencies(dependency)
        .flatMap { childDependency ->
            childDependency.moduleArtifacts.map {
                if (libraryFilter(it)) {
                    val cacheDirectory = getCacheDirectory(
                        rootCacheDirectory,
                        childDependency,
                        if (considerArtifact) it else null,
                        libraryFilter
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

fun getAllDependencies(dependency: ResolvedDependency): Set<ResolvedDependency> {
    val allDependencies = mutableSetOf<ResolvedDependency>()

    fun traverseAllDependencies(dependency: ResolvedDependency) {
        if (dependency in allDependencies)
            return
        allDependencies.add(dependency)
        dependency.children.forEach { traverseAllDependencies(it) }
    }

    dependency.children.forEach { traverseAllDependencies(it) }
    return allDependencies
}

internal class GradleLoggerAdapter(private val gradleLogger: Logger) : org.jetbrains.kotlin.util.Logger {
    override fun log(message: String) = gradleLogger.info(message)
    override fun warning(message: String) = gradleLogger.warn(message)
    override fun error(message: String) = kotlin.error(message)
    override fun fatal(message: String): Nothing = kotlin.error(message)
}
