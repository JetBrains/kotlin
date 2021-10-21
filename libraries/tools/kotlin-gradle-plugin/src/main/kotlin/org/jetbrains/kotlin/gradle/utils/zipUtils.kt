/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

internal fun copyZipFilePartially(sourceZipFile: File, destinationZipFile: File, path: String) {
    require(path.endsWith("/")) { "Expected path to end with '/', found '$path'" }

    ZipFile(sourceZipFile).use { zip ->
        val entries = zip.entries().asSequence()
            .filter { it.name.startsWith(path) }
            .filter { !it.isDirectory }.toList()

        if (entries.isEmpty()) return

        ZipOutputStream(destinationZipFile.outputStream()).use { destinationZipOutputStream ->
            entries.forEach { entry ->
                // Drop the source set name from the entry path
                val destinationEntry = ZipEntry(entry.name.substringAfter(path))

                zip.getInputStream(entry).use { inputStream ->
                    destinationZipOutputStream.putNextEntry(destinationEntry)
                    inputStream.copyTo(destinationZipOutputStream)
                    destinationZipOutputStream.closeEntry()
                }
            }
        }
    }
}

internal fun ZipFile.listDescendants(zipEntry: ZipEntry): Sequence<ZipEntry> {
    require(zipEntry.isDirectory)
    return entries().asSequence().filter { entry ->
        entry.name != zipEntry.name && entry.name.startsWith(zipEntry.name)
    }
}

internal fun ZipFile.listChildren(zipEntry: ZipEntry): Sequence<ZipEntry> {
    return listDescendants(zipEntry).filter { entry ->
        entry.name.removePrefix(zipEntry.name).count { it == '/' } == 1
    }
}
