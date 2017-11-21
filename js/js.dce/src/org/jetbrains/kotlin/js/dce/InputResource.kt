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

package org.jetbrains.kotlin.js.dce

import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.util.zip.ZipFile

class InputResource(val name: String, val lastModified: () -> Long, val reader: () -> InputStream) {
    companion object {
        fun file(path: String): InputResource = InputResource(path, { File(path).lastModified() }) { FileInputStream(File(path)) }

        fun zipFile(path: String, entryPath: String): InputResource =
                InputResource("$path!$entryPath", { getZipModificationTime(path, entryPath) }) {
                    val zipFile = ZipFile(path)
                    zipFile.getInputStream(zipFile.getEntry(entryPath))
                }

        private fun getZipModificationTime(path: String, entryPath: String): Long {
            val result = ZipFile(path).getEntry(entryPath).time
            return if (result != -1L) result else File(path).lastModified()
        }
    }
}