/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.code.review

interface LocalGit {
    suspend fun countCommitsUpTo(gitWorkingTree: GitWorkingTree, ancestor: GitRevision): Int

    suspend fun getMergeBase(gitWorkingTree: GitWorkingTree, revision: GitRevision): GitSHA1

    suspend fun getDiff(from: GitSHA1, to: GitWorkingTree): GitDiff

    suspend fun lsFiles(tree: GitWorkingTree): List<ProjectFilePath>
}

/**
 * An implementation of [LocalGit] that uses the `git` command-line tool.
 *
 * Alternatively, we could consider using JGit instead.
 */
object GitCLI : LocalGit {
    private suspend fun gitOutput(tree: GitWorkingTree, vararg arguments: String): String {
        val executionResult = runProcess(
            directory = tree.root,
            input = null,
            command = listOf("git") + arguments
        )
        executionResult.checkExitCode()
        return executionResult.stdout
    }

    override suspend fun countCommitsUpTo(gitWorkingTree: GitWorkingTree, ancestor: GitRevision): Int =
        gitOutput(gitWorkingTree, "rev-list", "--count", "${ancestor.rev}..HEAD").toInt()

    override suspend fun getMergeBase(gitWorkingTree: GitWorkingTree, revision: GitRevision): GitSHA1 =
        GitSHA1(gitOutput(gitWorkingTree, "merge-base", "HEAD", revision.rev))


    private const val DIFF_MINUS_MINUS_GIT = "diff --git "

    override suspend fun getDiff(from: GitSHA1, to: GitWorkingTree): GitDiff {
        val diffText = gitOutput(tree = to, "diff", "--default-prefix", from.sha1)
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

        return GitDiff(changedFiles, GitDiff.Origin.Local(from, to))
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

    override suspend fun lsFiles(tree: GitWorkingTree): List<ProjectFilePath> =
        gitOutput(tree, "ls-files").lines().map { ProjectFilePath(it) }
}
