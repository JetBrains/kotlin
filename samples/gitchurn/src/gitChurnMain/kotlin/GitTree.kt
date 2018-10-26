/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package sample.gitchurn

import kotlinx.cinterop.*
import libgit2.*

class GitTree(val repository: GitRepository, val handle: CPointer<git_tree>) {
    fun close() = git_tree_free(handle)

    fun entries(): List<GitTreeEntry> = memScoped {
        val size = git_tree_entrycount(handle).toInt()
        val entries = ArrayList<GitTreeEntry>(size)
        for (index in 0..size - 1) {
            val treeEntry = git_tree_entry_byindex(handle, index.convert())!!
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
