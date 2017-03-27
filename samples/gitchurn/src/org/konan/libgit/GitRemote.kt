package org.konan.libgit

import kotlinx.cinterop.*
import libgit2.*

class GitRemote(val repository: GitRepository, val handle: CPointer<git_remote>) {
    fun close() = git_remote_free(handle)

    val url: String get() = git_remote_url(handle)!!.toKString()
    val name: String = git_remote_name(handle)!!.toKString()
}