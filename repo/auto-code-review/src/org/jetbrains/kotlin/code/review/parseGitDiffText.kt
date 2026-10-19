/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.code.review

import org.eclipse.jgit.util.QuotedString

private const val DIFF_MINUS_MINUS_GIT = "diff --git "

fun parseGitDiffText(diffText: String): List<GitDiff.ChangedFile> {
    // Git separates lines with `\n`, while `lines()` would also split at `\r` in the file contents.
    val diffLines = ArrayDeque(diffText.split('\n'))
    // The last line is terminated too (unless the output has been trimmed), but no `git diff` line is empty.
    if (diffLines.last().isEmpty()) diffLines.removeLast()

    val changedFiles = mutableListOf<GitDiff.ChangedFile>()

    while (diffLines.isNotEmpty()) {
        val changedFileLines = listOf(diffLines.removeFirst()) +
                diffLines.removeFirstUntil { it.startsWith(DIFF_MINUS_MINUS_GIT) }

        changedFiles.add(parseChangedFile(changedFileLines))
    }

    return changedFiles
}

private const val DIFF_SRC_LINE_PREFIX = "--- "
private const val NEW_FILE_MODE_PREFIX = "new file mode "
private const val DELETED_FILE_MODE_PREFIX = "deleted file mode "
private const val RENAME_FROM_PREFIX = "rename from "
private const val RENAME_TO_PREFIX = "rename to "
private const val COPY_FROM_PREFIX = "copy from "
private const val COPY_TO_PREFIX = "copy to "

/**
 * See "Generating patch text with -p" in `git help diff`.
 *
 * The `---` and `+++` lines are present only if the file has text hunks
 * (so, not for a pure rename, a mode change, an empty or a binary file).
 * Therefore, the paths are taken from the `diff --git` line and the extended header lines.
 */
internal fun parseChangedFile(lines: List<String>): GitDiff.ChangedFile {
    val diffGitLine = lines.first()
    check(diffGitLine.startsWith(DIFF_MINUS_MINUS_GIT)) {
        """
            |In the `git diff` output,
            |expected a line starting with "$DIFF_MINUS_MINUS_GIT", but got:
            |$diffGitLine
        """.trimMargin()
    }

    // The extended header lines go before `---`. And if there is no `---`, then there are no hunks.
    val headerLines = lines.drop(1).takeWhile { !it.startsWith(DIFF_SRC_LINE_PREFIX) }

    fun findHeaderPath(prefix: String): String? =
        headerLines.firstOrNull { it.startsWith(prefix) }?.removePrefix(prefix)?.let(::unquoteGitPath)

    fun findHeaderPaths(fromPrefix: String, toPrefix: String): Pair<String, String>? {
        val fromPath = findHeaderPath(fromPrefix) ?: return null
        val toPath = checkNotNull(findHeaderPath(toPrefix)) {
            """
                |In the `git diff` output,
                |expected a line starting with "$toPrefix" after "$fromPrefix", but got:
                |${headerLines.joinToString("\n")}
            """.trimMargin()
        }
        return fromPath to toPath
    }

    // The `diff --git` line has different paths only if rename or copy is involved.
    // In this case, the line is ambiguous when the paths contain spaces, so the extended header lines are used instead.
    val [oldPath, newPath] = findHeaderPaths(RENAME_FROM_PREFIX, RENAME_TO_PREFIX)
        ?: findHeaderPaths(COPY_FROM_PREFIX, COPY_TO_PREFIX)
        ?: parseSamePathFromDiffGitLine(diffGitLine).let { it to it }

    val isAdded = headerLines.any { it.startsWith(NEW_FILE_MODE_PREFIX) }
    val isDeleted = headerLines.any { it.startsWith(DELETED_FILE_MODE_PREFIX) }

    return GitDiff.ChangedFile(
        oldPath = if (isAdded) null else ProjectFilePath(oldPath),
        newPath = if (isDeleted) null else ProjectFilePath(newPath),
        patchLines = lines,
    )
}

// Unless rename or copy is involved, the line is `diff --git a/<path> b/<path>` with the same <path> twice.
// It can't be split at a space, since <path> may contain spaces; the backreference `\1` finds the split.
// `DOT_MATCHES_ALL`: <path> may contain characters like U+2028, which `.` doesn't match by default.
private val SAME_PATH_TWICE = Regex("""a/(.*) b/\1""", RegexOption.DOT_MATCHES_ALL)
private val SAME_QUOTED_PATH_TWICE = Regex(""""a/(.*)" "b/\1"""", RegexOption.DOT_MATCHES_ALL)

private fun parseSamePathFromDiffGitLine(line: String): String {
    val paths = line.removePrefix(DIFF_MINUS_MINUS_GIT)
    SAME_QUOTED_PATH_TWICE.matchEntire(paths)?.let {
        return unquoteGitPath("\"${it.groupValues[1]}\"")
    }
    SAME_PATH_TWICE.matchEntire(paths)?.let {
        return it.groupValues[1]
    }
    error(
        """
            |In the `git diff` output,
            |expected a line "${DIFF_MINUS_MINUS_GIT}a/<path> b/<path>" with the same <path>, but got:
            |$line
        """.trimMargin()
    )
}

/**
 * Git quotes paths with "unusual" characters in the C style (see `core.quotePath` in `git help config`),
 * e.g. `"caf\303\251.txt"` for `café.txt`. Unquoted paths are returned as is.
 */
private fun unquoteGitPath(path: String): String = QuotedString.GIT_PATH.dequote(path)
