/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package sample.gitchurn

import kotlinx.cinterop.*
import libgit2.*

class GitRepository(val location: String) {
    val arena = Arena()
    val handle: CPointer<git_repository> = memScoped {
        val loc = allocPointerTo<git_repository>()
        git_repository_open(loc.ptr, location).errorCheck()
        loc.value!!
    }

    fun close() {
        git_repository_free(handle)
        arena.clear()
    }

    fun remotes(): List<GitRemote> = memScoped {
        val remoteList = alloc<git_strarray>()
        git_remote_list(remoteList.ptr, handle).errorCheck()
        val size = remoteList.count.toInt()
        val list = ArrayList<GitRemote>(size)
        for (index in 0..size - 1) {
            val array = remoteList.strings!!
            val name = array[index]!!.toKString()
            val remotePtr = allocPointerTo<git_remote>()
            git_remote_lookup(remotePtr.ptr, handle, name).errorCheck()
            list.add(GitRemote(this@GitRepository, remotePtr.value!!))
        }
        list
    }

    fun commits(): Sequence<GitCommit> = memScoped {
        val walkPtr = allocPointerTo<git_revwalk>()
        git_revwalk_new(walkPtr.ptr, handle).errorCheck()
        val walk = walkPtr.value
        git_revwalk_sorting(walk, GIT_SORT_TOPOLOGICAL or GIT_SORT_TIME)
        git_revwalk_push_head(walk).errorCheck()
        generateSequence<GitCommit> {
            memScoped {
                val oid = alloc<git_oid>()
                val result = git_revwalk_next(oid.ptr, walk)

                when (result) {
                    0 -> {
                        val commitPtr = allocPointerTo<git_commit>()
                        git_commit_lookup(commitPtr.ptr, handle, oid.ptr).errorCheck()
                        val commit = commitPtr.value!!
                        GitCommit(this@GitRepository, commit)
                    }
                    GIT_ITEROVER -> null
                    else -> throw Exception("Unexpected result code $result")
                }
            }
        }
    }
}