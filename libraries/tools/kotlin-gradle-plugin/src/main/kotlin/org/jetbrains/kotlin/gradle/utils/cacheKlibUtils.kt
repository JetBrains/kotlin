/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.logging.Logger
import java.io.File
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

fun getCacheDirectory(
    rootCacheDirectory: File,
    dependency: ResolvedDependency
): File {
    val moduleCacheDirectory = File(rootCacheDirectory, dependency.moduleName)
    val versionCacheDirectory = File(moduleCacheDirectory, dependency.moduleVersion)
    return File(versionCacheDirectory, computeDependenciesHash(dependency))
}

private fun ByteArray.toHexString() = joinToString("") { (0xFF and it.toInt()).toString(16).padStart(2, '0') }

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
    dependency: ResolvedDependency
): List<File>? {
    return getAllDependencies(dependency)
        .map { childDependency ->
            val hasKlibs = childDependency.moduleArtifacts.any { it.file.absolutePath.endsWith(".klib") }
            val cacheDirectory = getCacheDirectory(rootCacheDirectory, childDependency)
            // We can only compile klib to cache if all of its dependencies are also cached.
            if (hasKlibs && !cacheDirectory.exists())
                return null
            cacheDirectory
        }
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