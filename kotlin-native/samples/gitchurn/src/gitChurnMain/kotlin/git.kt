/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package sample.gitchurn

import kotlinx.cinterop.*
import libgit2.*

object git {
    init {
        git_libgit2_init()
    }

    fun close() {
        git_libgit2_shutdown()
    }

    fun repository(location: String): GitRepository {
        return GitRepository(location)
    }
}

fun Int.errorCheck() {
    if (this == 0) return
    throw GitException()
}

class GitException : Exception(run {
    val err = giterr_last()
    err!!.pointed.message!!.toKString()
})