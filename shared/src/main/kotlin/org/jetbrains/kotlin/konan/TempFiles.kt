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

import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.file.*

/**
 * Creates and stores temporary compiler outputs
 * If pathToTemporaryDir is given and is not empty then temporary outputs will be preserved
 */
class TempFiles(outputPath: String, pathToTemporaryDir: String? = null) {
    private val outputName = File(outputPath).name
    val deleteOnExit = pathToTemporaryDir == null || pathToTemporaryDir.isEmpty()

    val nativeBinaryFile    by lazy { create(outputName,".kt.bc") }
    val cAdapterCpp         by lazy { create("api", ".cpp") }
    val cAdapterBitcode     by lazy { create("api", ".bc") }

    val nativeBinaryFileName    get() = nativeBinaryFile.absolutePath
    val cAdapterCppName         get() = cAdapterCpp.absolutePath
    val cAdapterBitcodeName     get() = cAdapterBitcode.absolutePath

    private val dir by lazy {
        if (deleteOnExit) {
            createTempDir("konan_temp").deleteOnExit()
        } else {
            createDirForTemporaryFiles(pathToTemporaryDir!!)
        }
    }

    private fun createDirForTemporaryFiles(path: String): File {
        if (File(path).isFile) {
            throw IllegalArgumentException("Given file is not a directory: $path")
        }
        return File(path).apply {
            if (!exists) { mkdirs() }
        }
    }

    /**
     * Create file named {name}{suffix} inside temporary dir
     */
    fun create(prefix: String, suffix: String = ""): File =
            File(dir, "$prefix$suffix").also {
                if (deleteOnExit) it.deleteOnExit()
            }
}

