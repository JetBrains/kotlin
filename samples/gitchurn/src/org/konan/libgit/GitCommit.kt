/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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