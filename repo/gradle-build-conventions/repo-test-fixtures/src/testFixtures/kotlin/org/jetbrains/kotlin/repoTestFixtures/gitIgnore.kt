/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.repoTestFixtures

import org.eclipse.jgit.ignore.IgnoreNode
import java.nio.file.Path
import kotlin.io.path.*

private val repositoryRoot = Path("").absolute()

/**
 * Returns true if the path is ignored (as in listed in any '.gitignore').
 * @throws IllegalArgumentException if the path is not within the repository root.
 */
fun Path.isGitIgnored(): Boolean {
    val isDirectory = this.isDirectory()
    val thisPath = repositoryRoot.relativize(this.absolute()).invariantSeparatorsPathString
    if (thisPath == ".git") return true

    /*
    Resolve .gitignore files from parent directories to determine if the current path is git ignored.
     */
    var currentDirectory: Path? = parent?.absolute()
    while (currentDirectory != null) {
        val gitIgnoreNode = currentDirectory.resolve(".gitignore").getOrParseGitIgnoreNode()
        gitIgnoreNode?.checkIgnored(thisPath, isDirectory)?.let { result ->
            return result
        }

        currentDirectory = currentDirectory.parent
        if (currentDirectory == repositoryRoot) {
            break
        }
    }

    return false
}

private val ignoreNodesCache = hashMapOf<Path, IgnoreNode?>()

@Synchronized
private fun Path.getOrParseGitIgnoreNode(): IgnoreNode? = ignoreNodesCache.getOrPut(this) {
    if (!this.isRegularFile()) return@getOrPut null
    val node = IgnoreNode()
    inputStream().use { stream ->
        node.parse(repositoryRoot.relativize(this).invariantSeparatorsPathString, stream)
    }
    node
}
