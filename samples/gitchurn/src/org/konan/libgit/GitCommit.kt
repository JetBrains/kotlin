package org.konan.libgit

import kotlinx.cinterop.*
import libgit2.*

class GitCommit(val repository: GitRepository, val commit: CPointer<git_commit>) {
    fun close() = git_commit_free(commit)

    val summary: String get() = git_commit_summary(commit)!!.toKString()
    val time: git_time_t get() = git_commit_time(commit)

    val tree: GitTree get() = memScoped {
        val treePtr = allocPointerTo<git_tree>()
        git_commit_tree(treePtr.ptr, commit).errorCheck()
        GitTree(repository, treePtr.value!!)
    }

    val parents: List<GitCommit> get() = memScoped {
        val count = git_commit_parentcount(commit)
        val result = ArrayList<GitCommit>(count)
        for (index in 0..count - 1) {
            val commitPtr = allocPointerTo<git_commit>()
            git_commit_parent(commitPtr.ptr, commit, index).errorCheck()
            result.add(GitCommit(repository, commitPtr.value!!))
        }
        result
    }
}