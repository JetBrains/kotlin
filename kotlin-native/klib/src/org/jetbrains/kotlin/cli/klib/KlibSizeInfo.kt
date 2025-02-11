/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.klib

import org.jetbrains.kotlin.konan.file.file
import org.jetbrains.kotlin.konan.file.withZipFileSystem
import org.jetbrains.kotlin.konan.library.KLIB_TARGETS_FOLDER_NAME
import org.jetbrains.kotlin.library.IrKotlinLibraryLayout
import org.jetbrains.kotlin.library.KLIB_IR_FOLDER_NAME
import org.jetbrains.kotlin.library.KLIB_METADATA_FOLDER_NAME
import org.jetbrains.kotlin.library.KLIB_MANIFEST_FILE_NAME
import org.jetbrains.kotlin.library.KotlinLibrary
import java.util.LinkedList
import org.jetbrains.kotlin.konan.file.File as KFile

/**
 * [size] is always in bytes.
 */
internal class KlibElementWithSize private constructor(val name: String, val size: Long, val children: List<KlibElementWithSize>) {
    constructor(name: String, size: Long) : this(name, size, emptyList())
    constructor(name: String, children: List<KlibElementWithSize>) : this(name, children.sumOf { it.size }, children)
}

internal fun KotlinLibrary.loadSizeInfo(irInfo: KlibIrInfo?): KlibElementWithSize? {
    val libraryFile = libraryFile.absoluteFile

    return when {
        libraryFile.isFile -> KlibElementWithSize(
            "KLIB file cumulative size",
            libraryFile.withZipFileSystem { fs -> fs.file("/").collectTopLevelElements(irInfo) }
        )

        !libraryFile.isDirectory -> null

        else -> KlibElementWithSize(
            "KLIB directory cumulative size",
            libraryFile.collectTopLevelElements(irInfo)
        )
    }
}

private fun KFile.collectTopLevelElements(irInfo: KlibIrInfo?): List<KlibElementWithSize> {
    var defaultEntry: KFile? = null
    val otherTopLevelEntries = ArrayList<KFile>()

    for (entry in entries) {
        // Expand the contents of the "default" directory, don't show the directory itself.
        if (entry.name == "default" && entry.isDirectory) {
            defaultEntry = entry
        } else {
            otherTopLevelEntries += entry
        }
    }

    // The contents of the "default" entry go the first, then everything else.
    val topLevelEntries = buildList<KFile> {
        this += defaultEntry?.entries?.sortedBy(KFile::name).orEmpty()
        this += otherTopLevelEntries.sortedBy(KFile::name)
    }

    return topLevelEntries.map { topLevelEntry ->
        when (val topLevelEntryName = topLevelEntry.name) {
            KLIB_IR_FOLDER_NAME -> buildIrElement(topLevelEntry, irInfo)
            KLIB_METADATA_FOLDER_NAME -> buildElement(name = "Metadata", topLevelEntry)
            KLIB_TARGETS_FOLDER_NAME -> buildElement(name = "Native-specific binary data", topLevelEntry)
            KLIB_MANIFEST_FILE_NAME -> buildElement(name = "Manifest file", topLevelEntry)
            else -> buildElement(
                name = topLevelEntryName.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
                topLevelEntry
            )
        }
    }
}

private val KFile.entries: List<KFile> get() = listFiles

private fun KFile.cumulativeSize(): Long = when {
    isFile -> size
    isDirectory -> entries.sumOf { it.cumulativeSize() }
    else -> 0L
}

private fun buildElement(name: String, entry: KFile): KlibElementWithSize {
    return KlibElementWithSize(name, entry.cumulativeSize())
}

private fun buildIrElement(entry: KFile, irInfo: KlibIrInfo?): KlibElementWithSize {
    val nestedElements = ArrayList<KlibElementWithSize>()

    entry.entries.mapTo(nestedElements) { childEntry ->
        val prettyName = when (val childName = childEntry.name) {
            IrKotlinLibraryLayout.IR_FILES_FILE_NAME -> "IR files"
            IrKotlinLibraryLayout.IR_DECLARATIONS_FILE_NAME -> "IR declarations"
            IrKotlinLibraryLayout.IR_BODIES_FILE_NAME -> "IR bodies"
            IrKotlinLibraryLayout.IR_TYPES_FILE_NAME -> "IR types"
            IrKotlinLibraryLayout.IR_SIGNATURES_FILE_NAME -> "IR signatures"
            IrKotlinLibraryLayout.IR_DEBUG_INFO_FILE_NAME -> "IR signatures (debug info)"
            IrKotlinLibraryLayout.IR_STRINGS_FILE_NAME -> "IR strings"
            // TODO: add file entries here!
            else -> childName
        }

        buildElement(prettyName, childEntry)
    }

    irInfo?.meaningfulInlineFunctionBodiesSize?.let { estimationOfInlineBodiesSizes ->
        nestedElements += KlibElementWithSize("IR bodies (inline functions only)", estimationOfInlineBodiesSizes)
    }

    return KlibElementWithSize("IR", nestedElements.sortedBy { it.name })
}
