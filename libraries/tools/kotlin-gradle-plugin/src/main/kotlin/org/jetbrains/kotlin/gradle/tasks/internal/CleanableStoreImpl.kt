/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks.internal

import org.gradle.util.GFileUtils
import java.io.File
import java.nio.file.Files
import java.time.Instant

internal class CleanableStoreImpl(dirPath: String) : CleanableStore {
    private val dir = File(dirPath)

    override fun get(fileName: String): DownloadedFile =
        DownloadedFile(this, dir.resolve(fileName))

    override fun markUsed() {
        if (dir.exists()) {
            GFileUtils.touchExisting(dir)
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
}