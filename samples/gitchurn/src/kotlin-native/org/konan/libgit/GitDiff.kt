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
