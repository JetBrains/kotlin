/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.test.api.util

import org.jetbrains.dokka.utilities.DokkaLogger
import java.io.File
import java.io.IOException

/**
 * Converts a file path (like `org/jetbrains/dokka/test/File.kt`) to a fully qualified
 * package name (like `org.jetbrains.dokka.test`).
 *
 * @param srcRelativeFilePath path relative to the source code directory. If the full path of the file
 *                            is `src/main/kotlin/org/jetbrains/dokka/test/File.kt`, then only
 *                            `org/jetbrains/dokka/test/File.kt` is expected to be passed here.
 * @return fully qualified package name or an empty string in case of the root package.
 */
internal fun filePathToPackageName(srcRelativeFilePath: String): String {
    return srcRelativeFilePath
        .substringBeforeLast("/", missingDelimiterValue = "")
        .replace("/", ".")
}

/**
 * @throws IOException if the requested temporary directory could not be created or deleted once used.
 */
internal fun <T> withTempDirectory(logger: DokkaLogger? = null, block: (tempDirectory: File) -> T): T {
    @Suppress("DEPRECATION") // TODO migrate to kotlin.io.path.createTempDirectory with languageVersion >= 1.5
    val tempDir = createTempDir()
    try {
        logger?.debug("Created temporary directory $tempDir")
        return block(tempDir)
    } finally {
        if (!tempDir.deleteRecursively()) {
            throw IOException("Unable to delete temporary directory $tempDir")
        }
        logger?.debug("Deleted temporary directory $tempDir")
    }
}
