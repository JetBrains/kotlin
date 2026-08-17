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

    override suspend fun getDiff(from: GitSHA1, to: GitWorkingTree): GitDiff {
        val diffText = gitOutput(tree = to, "diff", "--default-prefix", from.sha1)
        val changedFiles = parseGitDiffText(diffText)

        return GitDiff(changedFiles, GitDiff.Origin.Local(from, to))
    }

    override suspend fun lsFiles(tree: GitWorkingTree): List<ProjectFilePath> =
        gitOutput(tree, "ls-files").lines().map { ProjectFilePath(it) }
}
