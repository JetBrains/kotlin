/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.generators.native.cache.version

import org.jetbrains.kotlin.generators.util.GeneratorsFileUtil
import java.nio.file.Paths
import java.util.logging.Logger

private val logger = Logger.getLogger("GenerateKotlinVersion")

fun main(args: Array<String>) {
    if (args.size < 3) {
        logger.severe("Error: Missing arguments. Usage: <generation_dir> <kotlin_version_string> <versions_file_path>")
        return
    }

    val genDirectoryPath = Paths.get(args[0])
    val supportedVersionsFilePath = Paths.get(args[2])
    val currentVersionString = args[1]

    val currentKotlinVersion = parseKotlinVersion(currentVersionString)

    logger.info("Gen dir: $genDirectoryPath")
    logger.info("Versions file: $supportedVersionsFilePath")
    logger.info("Current Kotlin version string: $currentVersionString")
    logger.info("-> Resolved to: $currentKotlinVersion")

    // 1. Update the intermediate versions file and get all versions
    val allKotlinVersions = NativeCacheKotlinVersionsFile.updateAndGetAll(supportedVersionsFilePath, currentKotlinVersion)
    logger.info("Total versions: ${allKotlinVersions.size}")

    // 2. Generate the .kt source files from the full list
    val (path, content) = NativeCacheKotlinVersionsGenerator.generate(allKotlinVersions)
    val genFile = genDirectoryPath.resolve(path)
    GeneratorsFileUtil.writeFileIfContentChanged(genFile.toFile(), content, logNotChanged = false)
}