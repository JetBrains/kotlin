/*
 * Copyright 2000-2017 JetBrains s.r.o.
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

package com.intellij.stats.storage

import com.intellij.openapi.application.PathManager
import java.io.File
import java.io.FileFilter
import java.nio.file.Files

/**
 * If you want to implement some other type of logging this is a goto class to temporarily store data locally, until it
 * will be sent to log service.
 *
 * @baseFileName, files will be named ${baseFileName}_{intIndex}
 * @rootDirectoryPath, root directory where folder named @logsDirectory will be created and all files will be stored
 * @logsDirectoryName, name of directory in root directory which will be used to store files
 */
open class UniqueFilesProvider(private val baseFileName: String,
                               private val rootDirectoryPath: String,
                               private val logsDirectoryName: String) : FilePathProvider() {
    private companion object {
        const val MAX_ALLOWED_SEND_SIZE = 2 * 1024 * 1024
    }

    override fun cleanupOldFiles() {
        val files = getDataFiles()
        val sizeToSend = files.fold(0L, { totalSize, file -> totalSize + file.length() })
        if (sizeToSend > MAX_ALLOWED_SEND_SIZE) {
            var currentSize = sizeToSend
            val iterator = files.iterator()
            while (iterator.hasNext() && currentSize > MAX_ALLOWED_SEND_SIZE) {
                val file = iterator.next()
                val fileSize = file.length()
                Files.delete(file.toPath())
                currentSize -= fileSize
            }
        }
    }

    override fun getUniqueFile(): File {
        val dir = getStatsDataDirectory()

        val currentMaxIndex = dir.filesOnly()
                .filter { it.name.startsWith(baseFileName) }
                .map { it.name.substringAfter('_') }
                .filter { it.isIntConvertable() }
                .map(String::toInt)
                .max()

        val newIndex = if (currentMaxIndex != null) currentMaxIndex + 1 else 0

        return File(dir, "${baseFileName}_$newIndex")
    }

    override fun getDataFiles(): List<File> {
        val dir = getStatsDataDirectory()
        return dir.filesOnly()
                .filter { it.name.startsWith(baseFileName) }
                .filter { it.name.substringAfter('_').isIntConvertable() }
                .sortedBy { it.getChunkNumber() }
                .toList()
    }

    override fun getStatsDataDirectory(): File {
        val dir = File(rootDirectoryPath, logsDirectoryName)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    private fun File.getChunkNumber() = this.name.substringAfter('_').toInt()

    private fun String.isIntConvertable(): Boolean {
        return try {
            this.toInt()
            true
        } catch (e: NumberFormatException) {
            false
        }
    }

    private fun File.filesOnly(): Sequence<File> {
        val files: Array<out File>? = this.listFiles(FileFilter { it.isFile })
        if (files == null) {
            val diagnostics = when {
                !exists() -> "file does not exist"
                !isDirectory -> "file is not a directory"
                isFile -> "file should be a directory but it is a file"
                else -> "unknown error"
            }

            throw Exception("Invalid directory path: ${this.relativeTo(File(PathManager.getSystemPath()))}. Info: $diagnostics")
        }

        return files.asSequence()
    }
}