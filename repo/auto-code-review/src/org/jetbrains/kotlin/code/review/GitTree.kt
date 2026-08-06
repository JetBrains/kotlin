/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.code.review

import java.io.File

open class GitRevision(val rev: String)
class GitSHA1(val sha1: String) : GitRevision(sha1)

class GitDiff(val changedFiles: List<ChangedFile>, val origin: Origin) {
    sealed class Origin {
        class Local(val from: GitSHA1, val to: GitWorkingTree) : Origin()
    }

    class ChangedFile(val oldPath: ProjectFilePath?, val newPath: ProjectFilePath?, val patchLines: List<String>) {
        init {
            check(oldPath != null || newPath != null) {
                "Both old and new paths are null:\n${patchLines.joinToString("\n")}"
            }
        }
    }
}

interface GitTree {
    val project: Project

    suspend fun getMergeBase(revision: GitRevision): GitSHA1

    suspend fun countCommitsAfter(ancestor: GitRevision): Int

    suspend fun getDiffFrom(revision: GitSHA1): GitDiff
}

class GitWorkingTree(val root: File, private val git: LocalGit) : GitTree {
    override val project = LocalProject(root)

    override suspend fun getMergeBase(revision: GitRevision): GitSHA1 = git.getMergeBase(this, revision)
    override suspend fun countCommitsAfter(ancestor: GitRevision): Int = git.countCommitsUpTo(this, ancestor)
    override suspend fun getDiffFrom(revision: GitSHA1): GitDiff = git.getDiff(from = revision, to = this)

    suspend fun lsFiles(): List<ProjectFilePath> = git.lsFiles(this)
}
