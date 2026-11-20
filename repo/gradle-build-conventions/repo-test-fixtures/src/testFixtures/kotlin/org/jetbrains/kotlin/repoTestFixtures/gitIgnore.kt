/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.repoTestFixtures

import org.eclipse.jgit.ignore.IgnoreNode
import org.jetbrains.kotlin.testFederation.RepositoryPath
import java.nio.channels.Channels
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.io.path.absolute
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.isDirectory

internal fun RepositoryPath.isGitIgnored(): Boolean {
    val absoluteRepositoryRoot = root.absolute().normalize()
    val absolutePath = resolve().absolute().normalize()

    require(absolutePath.startsWith(absoluteRepositoryRoot)) {
        "Path '$this' is not within the repository root '$root'"
    }

    val relativePath = absoluteRepositoryRoot.relativize(absolutePath)
    if (relativePath.any { it.toString() == ".git" }) return true

    val ignoreNodes = buildList {
        var currentDirectory = if (absolutePath.isDirectory()) absolutePath else absolutePath.parent
        while (currentDirectory != null) {
            val node = currentDirectory.resolve(".gitignore").getOrParseGitIgnoreNode(absoluteRepositoryRoot)
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
private fun Path.getOrParseGitIgnoreNode(repositoryRoot: Path): IgnoreNode? {
    val key = IgnoreNodeCacheKey(repositoryRoot, this)
    val value = ignoreNodesCache[key]
    if (value != null || key in ignoreNodesCache) {
        return value
    }

    val ignoreNode = if (Files.isRegularFile(this, LinkOption.NOFOLLOW_LINKS)) {
        IgnoreNode().also { node ->
            Files.newByteChannel(this, setOf(StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)).use { channel ->
                Channels.newInputStream(channel).use { stream ->
                    node.parse(repositoryRoot.relativize(this).invariantSeparatorsPathString, stream)
                }
            }
        }
    } else {
        null
    }

    ignoreNodesCache[key] = ignoreNode
    return ignoreNode
}
