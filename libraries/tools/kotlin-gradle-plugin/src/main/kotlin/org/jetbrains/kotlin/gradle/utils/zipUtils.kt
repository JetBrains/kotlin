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
    requireValidZipPath(path)

    ZipFile(sourceZipFile).use { zip ->
        val entries = zip.listDescendants(path).toList()
        if (entries.isEmpty()) return

        ZipOutputStream(destinationZipFile.outputStream()).use { destinationZipOutputStream ->
            entries.forEach { sourceEntry ->
                val destinationEntry = ZipEntry(sourceEntry.name.substringAfter(path))
                destinationZipOutputStream.putNextEntry(destinationEntry)

                if (!sourceEntry.isDirectory) {
                    zip.getInputStream(sourceEntry).use { inputStream ->
                        inputStream.copyTo(destinationZipOutputStream)
                    }
                }

                destinationZipOutputStream.closeEntry()
            }
        }
    }
}

internal fun ZipFile.listDescendants(path: String): Sequence<ZipEntry> {
    requireValidZipPath(path)
    return entries().asSequence().filter { entry ->
        entry.name != path && entry.name.startsWith(path)
    }
}

private fun requireValidZipPath(path: String) = require(path.isEmpty() || path.endsWith("/")) {
    "Expected path to end with '/', found '$path'"
}
