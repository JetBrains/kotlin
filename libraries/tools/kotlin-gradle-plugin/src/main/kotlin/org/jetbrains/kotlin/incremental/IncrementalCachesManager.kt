/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.modules.TargetId
import java.io.File

internal class IncrementalCachesManager (
        private val targetId: TargetId,
        private val cacheDirectory: File,
        private val outputDir: File
) {
    private val incrementalCacheDir = File(cacheDirectory, "increCache.${targetId.name}")
    private val lookupCacheDir = File(cacheDirectory, "lookups")
    private var incrementalCacheOpen = false
    private var lookupCacheOpen = false

    val incrementalCache: GradleIncrementalCacheImpl by lazy {
        val cache = GradleIncrementalCacheImpl(targetDataRoot = incrementalCacheDir.apply { mkdirs() }, targetOutputDir = outputDir, target = targetId)
        incrementalCacheOpen = true
        cache
    }

    val lookupCache: LookupStorage by lazy {
        val cache = LookupStorage(lookupCacheDir.apply { mkdirs() })
        lookupCacheOpen = true
        cache
    }

    fun clean() {
        close(flush = false)
        cacheDirectory.deleteRecursively()
    }

    fun close(flush: Boolean = false) {
        if (incrementalCacheOpen) {
            if (flush) {
                incrementalCache.flush(false)
            }
            incrementalCache.close()
            incrementalCacheOpen = false
        }

        if (lookupCacheOpen) {
            if (flush) {
                lookupCache.flush(false)
            }
            lookupCache.close()
            lookupCacheOpen = false
        }
    }
}