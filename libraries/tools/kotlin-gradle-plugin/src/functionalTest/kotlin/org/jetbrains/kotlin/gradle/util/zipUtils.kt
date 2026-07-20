/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.util

import org.jetbrains.kotlin.gradle.internal.ensureParentDirsCreated
import org.jetbrains.kotlin.gradle.utils.use
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import kotlin.sequences.forEach
import kotlin.streams.asSequence


internal fun zipTo(destination: File, contentDirectory: File) {
    if (!contentDirectory.isDirectory) error("$contentDirectory is not a directory")
    ZipOutputStream(destination.outputStream().buffered()).use { out ->
        contentDirectory.walkTopDown().forEach { file ->
            val path = file.relativeTo(contentDirectory).path
            if (file.isDirectory) {
                out.putNextEntry(ZipEntry("$path/"))
                return@forEach
            }

            if (file.isFile) {
                out.putNextEntry(ZipEntry(path))
                file.inputStream().buffered().use { input -> input.copyTo(out) }
                out.closeEntry()
            }
        }
    }
}

internal fun unzipTo(destination: File, zipFile: File) {
    ZipFile(zipFile).use { zip ->
        zip.stream().asSequence().forEach { entry ->
            val target = destination.resolve(entry.name)
            if (entry.isDirectory) {
                target.mkdir()
            } else {
                @Suppress("DEPRECATION")
                target.ensureParentDirsCreated()
                target.outputStream().buffered().use { output -> zip.getInputStream(entry).buffered().copyTo(output) }
            }
        }
    }
}
