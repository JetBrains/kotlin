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

class GitRemote(val repository: GitRepository, val handle: CPointer<git_remote>) {
    fun close() = git_remote_free(handle)

    val url: String get() = git_remote_url(handle)!!.toKString()
    val name: String = git_remote_name(handle)!!.toKString()
}