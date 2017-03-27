package org.konan.libgit

import kotlinx.cinterop.*
import libgit2.*

class GitDiff(val repository: GitRepository, val handle: CPointer<git_diff>) {
    fun deltas(): List<GifDiffDelta> {
        val size = git_diff_num_deltas(handle)
        val results = ArrayList<GifDiffDelta>(size.toInt())
        for (index in 0..size - 1) {
            val delta = git_diff_get_delta(handle, index)
            results.add(GifDiffDelta(this, delta!!))
        }
        return results
    }

    fun close() {
        git_diff_free(handle)
    }

}

class GifDiffDelta(val diff: GitDiff, val handle: CPointer<git_diff_delta>) {

    val status get() = handle.pointed.status.value
    val newPath get() = handle.pointed.new_file.path.value!!.toKString()
    val oldPath get() = handle.pointed.old_file.path.value!!.toKString()

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
