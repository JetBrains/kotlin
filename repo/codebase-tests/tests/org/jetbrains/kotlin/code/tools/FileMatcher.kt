/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.code.tools

import org.jetbrains.kotlin.repoTestFixtures.isGitIgnored
import java.io.File
import java.util.regex.Pattern
import kotlin.io.invariantSeparatorsPath
import kotlin.text.startsWith

internal class FileMatcher(val root: File, paths: Collection<String>) {
    private val files = paths.map { File(it) }
    private val paths = files.mapTo(HashSet()) { it.invariantSeparatorsPath }
    private val relativePaths = files.filterTo(ArrayList()) { it.isDirectory }.mapTo(HashSet()) { it.invariantSeparatorsPath + "/" }

    private fun File.invariantRelativePath() = relativeTo(root).invariantSeparatorsPath

    fun matchExact(file: File): Boolean {
        return file.invariantRelativePath() in paths
    }

    fun matchWithContains(file: File): Boolean {
        if (matchExact(file)) return true
        val relativePath = file.invariantRelativePath()
        return relativePaths.any { relativePath.startsWith(it) }
    }

    fun unmatched(files: List<File>): Set<String> {
        val filePaths = files.map { it.invariantRelativePath() }.toSet()
        val relativePaths = paths.filter { p -> filePaths.any { it.startsWith(p) } }.toSet()
        return paths - filePaths - relativePaths
    }
}

internal fun FileMatcher.excludeWalkTopDown(filePattern: Pattern): Sequence<File> {
    return root.walkTopDown()
        .onEnter { dir ->
            !matchExact(dir) && !dir.toPath().isGitIgnored() // Don't enter to ignored dirs
        }
        .filter { file -> !matchExact(file) } // filter ignored files
        .filter { file -> filePattern.matcher(file.name).matches() }
        .filter { file -> file.isFile }
}
