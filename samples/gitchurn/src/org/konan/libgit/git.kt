package org.konan.libgit

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
    err!!.pointed.message.value!!.toKString()
})