/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks.internal

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.UncheckedIOException
import java.nio.file.Files
import java.nio.file.attribute.FileTime
import java.time.Instant

internal class CleanableStoreImpl(dirPath: String) : CleanableStore {
    private val dir = File(dirPath)

    override fun get(fileName: String): DownloadedFile =
        DownloadedFile(this, dir.resolve(fileName))

    override fun markUsed() {
        if (dir.exists()) {
            touchExisting(dir)
        }
    }

    override fun cleanDir(expirationDate: Instant) {
        fun modificationDate(file: File): Instant {
            return Files.getLastModifiedTime(file.toPath()).toInstant()
        }

        dir.listFiles()
            ?.filter { file ->
                modificationDate(file).isBefore(expirationDate)
            }
            ?.forEach { file -> file.deleteRecursively() }
    }

    private fun touchExisting(file: File) {
        try {
            Files.setLastModifiedTime(file.toPath(), FileTime.fromMillis(System.currentTimeMillis()))
        } catch (e: IOException) {
            if (file.isFile && file.length() == 0L) {
                // On Linux, users cannot touch files they don't own but have write access to
                // because the JDK uses futimes() instead of futimens() [note the 'n'!]
                // see https://github.com/gradle/gradle/issues/7873
                touchFileByWritingEmptyByteArray(file)
            } else {
                throw UncheckedIOException("Could not update timestamp for $file", e)
            }
        }
    }

    private fun touchFileByWritingEmptyByteArray(file: File) {
        try {
            FileOutputStream(file).use { it.write(ByteArray(0)) }
        } catch (e: IOException) {
            throw UncheckedIOException("Could not update timestamp for $file", e)
        }
    }
}