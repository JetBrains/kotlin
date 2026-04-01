/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.generators.native.cache.version

import org.jetbrains.kotlin.generators.util.GeneratorsFileUtil
import java.nio.file.Path
import java.util.logging.Logger
import kotlin.io.path.exists
import kotlin.io.path.readLines

internal object NativeCacheKotlinVersionsFile {
    private val logger = Logger.getLogger(NativeCacheKotlinVersionsFile::class.java.name)

    /**
     * Reads the [versionsFilePath] file, adds the current version if it's not present,
     * and writes the updated, sorted list back to the file.
     *
     * @return A sorted Set of all known Kotlin version entries.
     */
    fun updateAndGetAll(
        versionsFilePath: Path,
        currentVersion: Triple<Int, Int, Int>,
    ): Set<Triple<Int, Int, Int>> {

        // 1. Read existing versions from the file
        val allVersions = mutableSetOf<Triple<Int, Int, Int>>()
        if (versionsFilePath.exists()) {
            versionsFilePath.readLines()
                .filter { it.isNotBlank() }
                .mapTo(allVersions) { line ->
                    try {
                        parseKotlinVersion(line)
                    } catch (_: Exception) {
                        error("Failed to parse version '$line' from $versionsFilePath.")
                    }
                }
        }

        // 2. Add the current version. 'set.add()' returns true if the item was new.
        if (allVersions.add(currentVersion)) {
            logger.info("New version $currentVersion detected. Adding to $versionsFilePath.")

            // 3. Sort and write the complete list back to the file
            val sortedVersionNames = allVersions
                .sortedWith(compareBy({ it.first }, { it.second }, { it.third }))
                .joinToString(separator = "\n") { "v${it.first}_${it.second}_${it.third}" }

            GeneratorsFileUtil.writeFileIfContentChanged(versionsFilePath.toFile(), sortedVersionNames)

            logger.info("Updated $versionsFilePath with new version.")
        } else {
            logger.info("Version $currentVersion is already present in $versionsFilePath.")
        }

        return allVersions.toSortedSet(compareBy({ it.first }, { it.second }, { it.third }))
    }
}