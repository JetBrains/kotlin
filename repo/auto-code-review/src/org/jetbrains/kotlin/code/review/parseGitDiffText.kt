/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.code.review

private const val DIFF_MINUS_MINUS_GIT = "diff --git "

fun parseGitDiffText(diffText: String): List<GitDiff.ChangedFile> {
    val diffLines = ArrayDeque(diffText.lines())

    val changedFiles = mutableListOf<GitDiff.ChangedFile>()

    while (diffLines.isNotEmpty()) {
        val firstLine = diffLines.first()
        check(firstLine.startsWith(DIFF_MINUS_MINUS_GIT)) {
            """
                |In the `git diff` output,
                |expected a line starting with "$DIFF_MINUS_MINUS_GIT", but got:
                |$firstLine
            """.trimMargin()
        }

        val changedFileLines = listOf(diffLines.removeFirst()) +
                diffLines.removeFirstUntil { it.startsWith(DIFF_MINUS_MINUS_GIT) }

        changedFiles.add(parseChangedFile(changedFileLines))
    }

    return changedFiles
}

private const val DIFF_SRC_LINE_PREFIX = "--- "
private const val DIFF_DST_LINE_PREFIX = "+++ "

private fun parseChangedFile(lines: List<String>): GitDiff.ChangedFile {
    // It starts with `diff --git`.
    // Then goes an arbitrary number of header lines. Look for `---` and `+++`.
    val oldFileLineIndex = lines.indexOfFirst { it.startsWith(DIFF_SRC_LINE_PREFIX) }
    check(oldFileLineIndex != -1)
    val oldFile = lines[oldFileLineIndex].removePrefix(DIFF_SRC_LINE_PREFIX)
        .takeIf { it != "/dev/null" }?.removePrefix("a/")?.let(::ProjectFilePath)

    val newFileLineIndex = oldFileLineIndex + 1
    val newFileLine = lines.getOrNull(newFileLineIndex)
    check(newFileLine?.startsWith(DIFF_DST_LINE_PREFIX) == true) {
        """
            |In the `git diff` output,
            |expected a line starting with "$DIFF_DST_LINE_PREFIX", but got:
            |$newFileLine
        """.trimMargin()
    }
    val newFile = newFileLine.removePrefix(DIFF_DST_LINE_PREFIX)
        .takeIf { it != "/dev/null" }?.removePrefix("b/")?.let(::ProjectFilePath)

    return GitDiff.ChangedFile(
        oldPath = oldFile,
        newPath = newFile,
        patchLines = lines,
    )
}
