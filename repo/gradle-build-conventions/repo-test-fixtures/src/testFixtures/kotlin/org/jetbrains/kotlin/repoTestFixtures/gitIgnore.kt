/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.repoTestFixtures

import org.eclipse.jgit.ignore.IgnoreNode
import java.nio.channels.Channels
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.io.path.Path
import kotlin.io.path.absolute
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.isDirectory

private val repositoryRoot by lazy {
    findRepositoryRoot(Path("").absolute())
        ?: error("Cannot find the repository root from the current working directory")
}

/**
 * Returns true if the path is ignored (as in listed in any '.gitignore').
 * @throws IllegalArgumentException if the path is not within the repository root.
 */
fun Path.isGitIgnored(): Boolean = (if (isAbsolute) this else repositoryRoot.resolve(this)).isGitIgnored(repositoryRoot)

internal fun Path.isGitIgnored(repositoryRoot: Path): Boolean {
    val absoluteRepositoryRoot = repositoryRoot.absolute().normalize()
    val absolutePath = (if (isAbsolute) this else absoluteRepositoryRoot.resolve(this)).absolute().normalize()

    require(absolutePath.startsWith(absoluteRepositoryRoot)) {
        "Path '$this' is not within the repository root '$repositoryRoot'"
    }

    val relativePath = absoluteRepositoryRoot.relativize(absolutePath)
    if (relativePath.any { it.toString() == ".git" }) return true

    val ignoreNodes = buildList {
        var currentDirectory = if (absolutePath.isDirectory()) absolutePath else absolutePath.parent
        while (currentDirectory != null) {
            val node = currentDirectory.resolve(".gitignore").getOrParseGitIgnoreNode(repositoryRoot)
            if (node != null) add(IgnoreNodeInDirectory(currentDirectory, node))

            if (currentDirectory == absoluteRepositoryRoot) break
            currentDirectory = currentDirectory.parent
        }
    }

    return ignoreNodes.checkIgnored(absolutePath, absolutePath.isDirectory()) ?: false
}

internal fun findRepositoryRoot(startingPath: Path): Path? {
    var currentPath: Path? = startingPath.absolute().normalize()
    while (currentPath != null) {
        val gitMetadata = currentPath.resolve(".git")
        if (Files.isDirectory(gitMetadata, LinkOption.NOFOLLOW_LINKS) ||
            Files.isRegularFile(gitMetadata, LinkOption.NOFOLLOW_LINKS)
        ) {
            return currentPath
        }
        currentPath = currentPath.parent
    }
    return null
}

private class IgnoreNodeInDirectory(val directory: Path, private val node: IgnoreNode) {
    fun checkIgnored(path: Path, isDirectory: Boolean): Boolean? = synchronized(node) {
        node.checkIgnored(directory.relativize(path).invariantSeparatorsPathString, isDirectory)
    }
}

private fun List<IgnoreNodeInDirectory>.checkIgnored(path: Path, isDirectory: Boolean): Boolean? {
    forEach { node ->
        node.checkIgnored(path, isDirectory)?.let { return it }
    }
    return null
}

private val ignoreNodesCache = hashMapOf<IgnoreNodeCacheKey, IgnoreNode?>()

data class IgnoreNodeCacheKey(val repositoryRoot: Path, val path: Path)

@Synchronized
private fun Path.getOrParseGitIgnoreNode(repositoryRoot: Path): IgnoreNode? =
    ignoreNodesCache.getOrPut(IgnoreNodeCacheKey(repositoryRoot, this)) {
        if (!Files.isRegularFile(this, LinkOption.NOFOLLOW_LINKS)) return@getOrPut null
        val node = IgnoreNode()
        Files.newByteChannel(this, setOf(StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)).use { channel ->
            Channels.newInputStream(channel).use { stream ->
                node.parse(repositoryRoot.relativize(this).invariantSeparatorsPathString, stream)
            }
        }
        node
    }
