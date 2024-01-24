/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
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

/**
 * Finds a resource by [resourcePath], and returns its absolute path.
 *
 * A resource is usually a file found in the `resources` directory of the project.
 *
 * For example, if you have a file `project/src/main/resources/jars/kotlinx-cli-jvm-0.3.6.jar`,
 * you should be able to get it by calling this function as
 * `getResourceAbsolutePath("jars/kotlinx-cli-jvm-0.3.6.jar")`.
 *
 * @throws IllegalArgumentException if the resource cannot be found or does not exist
 * @return an absolute path to the resource, such as `/home/user/projects/dokka/../MyFile.md`
 */
fun getResourceAbsolutePath(resourcePath: String): String {
    val resourceFile = getResourceFile(resourcePath)
    require(resourceFile.exists()) {
        "Resource file does not exist: $resourcePath"
    }
    return resourceFile.absolutePath
}

private fun getResourceFile(resourcePath: String): File {
    val resource = object {}.javaClass.classLoader.getResource(resourcePath)?.file
        ?: throw IllegalArgumentException("Resource not found: $resourcePath")

    return File(resource)
}
