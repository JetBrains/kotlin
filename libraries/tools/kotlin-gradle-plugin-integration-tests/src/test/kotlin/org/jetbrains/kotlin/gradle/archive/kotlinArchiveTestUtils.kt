/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.archive

import org.apache.commons.compress.compressors.xz.XZCompressorInputStream
import java.io.File
import java.nio.file.Path
import java.util.zip.ZipInputStream

internal fun Path.zipXzArchiveEntries(): List<String> = toFile().useKotlinArchive { zipInput ->
    generateSequence(zipInput::getNextEntry).map { entry -> entry.name }.toList()
}

/**
 * Reads one entry of a `.kar.xz` archive as text, or returns null when the entry is absent.
 */
internal fun File.readKotlinArchiveEntry(entryPath: String): String? = useKotlinArchive { zipInput ->
    generateSequence(zipInput::getNextEntry)
        .firstOrNull { entry -> entry.name == entryPath }
        ?.let { zipInput.readBytes().toString(Charsets.UTF_8) }
}

private inline fun <T> File.useKotlinArchive(block: (ZipInputStream) -> T): T =
    inputStream().buffered().use { fileInput ->
        XZCompressorInputStream(fileInput).use { xzInput ->
            ZipInputStream(xzInput).use(block)
        }
    }
