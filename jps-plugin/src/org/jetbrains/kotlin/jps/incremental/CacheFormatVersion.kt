/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.jps.incremental

import org.jetbrains.kotlin.config.IncrementalCompilation
import org.jetbrains.kotlin.jps.build.KotlinBuilder
import org.jetbrains.kotlin.load.java.JvmAbi
import java.io.File

class CacheFormatVersion(targetDataRoot: File) {
    companion object {
        // Change this when incremental cache format changes
        private val INCREMENTAL_CACHE_OWN_VERSION = 5

        private val CACHE_FORMAT_VERSION =
                INCREMENTAL_CACHE_OWN_VERSION * 1000000 +
                JvmAbi.VERSION.major * 1000 +
                JvmAbi.VERSION.minor

        private val NON_INCREMENTAL_MODE_PSEUDO_VERSION = Int.MAX_VALUE

        val FORMAT_VERSION_FILE_PATH: String = "$CACHE_DIRECTORY_NAME/format-version.txt"
    }

    private val file = File(targetDataRoot, FORMAT_VERSION_FILE_PATH)

    private fun actualCacheFormatVersion() = if (IncrementalCompilation.ENABLED) CACHE_FORMAT_VERSION else NON_INCREMENTAL_MODE_PSEUDO_VERSION

    public fun isIncompatible(): Boolean {
        if (!file.exists()) return false

        val versionNumber = file.readText().toInt()
        val expectedVersionNumber = actualCacheFormatVersion()

        if (versionNumber != expectedVersionNumber) {
            KotlinBuilder.LOG.info("Incompatible incremental cache version, expected $expectedVersionNumber, actual $versionNumber")
            return true
        }
        return false
    }

    fun saveIfNeeded() {
        if (file.parentFile.exists() && !file.exists()) {
            file.writeText(actualCacheFormatVersion().toString())
        }
    }

    fun clean() {
        file.delete()
    }
}