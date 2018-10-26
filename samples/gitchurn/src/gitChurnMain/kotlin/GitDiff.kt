/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package sample.gitchurn

import kotlinx.cinterop.*
import libgit2.*

class GitDiff(val repository: GitRepository, val handle: CPointer<git_diff>) {
    fun deltas(): List<GifDiffDelta> {
        val size = git_diff_num_deltas(handle).toInt()
        val results = ArrayList<GifDiffDelta>(size)
        for (index in 0..size - 1) {
            val delta = git_diff_get_delta(handle, index.convert())
            results.add(GifDiffDelta(this, delta!!))
        }
        return results
    }

    fun close() {
        git_diff_free(handle)
    }

}

class GifDiffDelta(val diff: GitDiff, val handle: CPointer<git_diff_delta>) {

    val status get() = handle.pointed.status
    val newPath get() = handle.pointed.new_file.path!!.toKString()
    val oldPath get() = handle.pointed.old_file.path!!.toKString()

    fun status(): String {
        return when (status) {
            GIT_DELTA_ADDED -> "A"
            GIT_DELTA_DELETED -> "D"
            GIT_DELTA_MODIFIED -> "M"
            GIT_DELTA_RENAMED -> "R"
            GIT_DELTA_COPIED -> "C"
            else -> throw Exception("Unsupported delta status $status")
        }
    }

}
