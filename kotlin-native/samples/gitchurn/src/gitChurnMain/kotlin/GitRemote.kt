/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package sample.gitchurn

import kotlinx.cinterop.*
import libgit2.*

class GitRemote(val repository: GitRepository, val handle: CPointer<git_remote>) {
    fun close() = git_remote_free(handle)

    val url: String get() = git_remote_url(handle)!!.toKString()
    val name: String = git_remote_name(handle)!!.toKString()
}