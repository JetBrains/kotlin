/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.util

import java.io.File

/**
 * An interface for extracting archive files.
 */
interface ArchiveExtractor {

    /**
     * Extracts the contents of the specified archive file to the target directory.
     *
     * @param archive The archive file to extract.
     * @param targetDirectory The directory where the contents of the archive will be extracted to.
     * @param archiveType The type of the archive file.
     */
    fun extract(archive: File, targetDirectory: File, archiveType: ArchiveType)
}