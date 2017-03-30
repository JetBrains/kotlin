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

class GitTree(val repository: GitRepository, val handle: CPointer<git_tree>) {
    fun close() = git_tree_free(handle)

    fun entries(): List<GitTreeEntry> = memScoped {
        val size = git_tree_entrycount(handle)
        val entries = ArrayList<GitTreeEntry>(size.toInt())
        for (index in 0..size - 1) {
            val treeEntry = git_tree_entry_byindex(handle, index)!!
            val entryType = git_tree_entry_type(treeEntry)
            val entry = when (entryType) {
                GIT_OBJ_TREE -> memScoped {
                    val id = git_tree_entry_id(treeEntry)
                    val treePtr = allocPointerTo<git_tree>()
                    git_tree_lookup(treePtr.ptr, repository.handle, id)
                    GitTreeEntry.Folder(this@GitTree, treeEntry, treePtr.value!!)
                }
                GIT_OBJ_BLOB -> GitTreeEntry.File(this@GitTree, treeEntry)
                else -> throw Exception("Unsupported entry type $entryType")
            }
            entries.add(entry)
        }
        entries
    }

    fun diff(other: GitTree): GitDiff = memScoped {
        val diffPtr = allocPointerTo<git_diff>()
        git_diff_tree_to_tree(diffPtr.ptr, repository.handle, handle, other.handle, null).errorCheck()
        GitDiff(repository, diffPtr.value!!)
    }
}

sealed class GitTreeEntry(val tree: GitTree, val handle: CPointer<git_tree_entry>) {
    val name: String get() = git_tree_entry_name(handle)!!.toKString()

    class Folder(tree: GitTree, handle: CPointer<git_tree_entry>, val subtreeHandle: CPointer<git_tree>) : GitTreeEntry(tree, handle) {
        val subtree = GitTree(tree.repository, subtreeHandle)
    }

    class File(tree: GitTree, handle: CPointer<git_tree_entry>) : GitTreeEntry(tree, handle)
}
