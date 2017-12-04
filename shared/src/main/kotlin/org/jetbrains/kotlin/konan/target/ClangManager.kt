/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed -> in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.konan.target

import org.jetbrains.kotlin.konan.properties.*

class ClangManager(val properties: Properties, val baseDir: String) {
    private val host = TargetManager.host
    private val enabledTargets = TargetManager.enabled
    private val konanProperties = enabledTargets.map {
        it to KonanProperties(it, properties, baseDir)
    }.toMap()

    private val targetClangArgs = enabledTargets.map {
        it to ClangTargetArgs(it, konanProperties[it]!!) 
    }.toMap()

    private val hostClang = ClangHostArgs(konanProperties[host]!!)

    // These are converted to arrays to be convenient
    // in groovy plugins.
    val hostClangArgs = (hostClang.commonClangArgs + targetClangArgs[host]!!.specificClangArgs).toTypedArray()

    fun targetClangArgs(target: KonanTarget)
        = (hostClang.commonClangArgs + targetClangArgs[target]!!.specificClangArgs).toTypedArray()

    val hostCompilerArgsForJni = hostClang.hostCompilerArgsForJni.toTypedArray()
}

