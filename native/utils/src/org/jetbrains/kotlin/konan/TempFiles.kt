/*
 * Copyright 2010-2018 JetBrains s.r.o.
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
package org.jetbrains.kotlin.konan

import kotlin.io.path.Path
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempDirectory
import kotlin.io.path.deleteRecursively
import kotlin.io.path.exists
import kotlin.io.path.isDirectory

/**
 * Creates and stores temporary compiler outputs
 * If pathToTemporaryDir is given and is not empty then temporary outputs will be preserved
 */
class TempFiles(pathToTemporaryDir: String? = null) {
    @OptIn(ExperimentalPathApi::class)
    fun dispose() {
        if (deleteOnExit) {
            // Note: this can throw an exception if a file deletion is failed for some reason (e.g. OS is Windows and the file is in use).
            dir.deleteRecursively()
        }
    }

    val deleteOnExit = pathToTemporaryDir.isNullOrEmpty()

    private val dir by lazy {
        if (deleteOnExit) {
            createTempDirectory("konan_temp")
        } else {
            createDirForTemporaryFiles(pathToTemporaryDir!!)
        }
    }

    private fun createDirForTemporaryFiles(rawPath: String): Path {
        val path = Path(rawPath)
        if (path.exists() && !path.isDirectory()) {
            throw IllegalArgumentException("Given file is not a directory: $rawPath")
        }

        path.createDirectories()

        return path
    }

    /**
     * Create file named {name}{suffix} inside temporary dir
     */
    fun create(prefix: String, suffix: String = ""): Path = dir.resolve("$prefix$suffix")
}

