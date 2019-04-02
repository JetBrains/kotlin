/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.library.lite

import org.jetbrains.kotlin.konan.library.KLIB_DIR_NAME
import org.jetbrains.kotlin.konan.library.KLIB_MANIFEST_FILE_NAME
import org.jetbrains.kotlin.konan.library.KLIB_PROPERTY_COMPILER_VERSION
import org.jetbrains.kotlin.konan.library.KLIB_PROPERTY_UNIQUE_NAME
import org.jetbrains.kotlin.konan.library.KONAN_COMMON_LIBS_DIR_NAME
import org.jetbrains.kotlin.konan.library.KONAN_PLATFORM_LIBS_DIR_NAME
import org.jetbrains.kotlin.konan.library.KONAN_SOURCES_DIR_NAME
import org.jetbrains.kotlin.konan.library.KONAN_STDLIB_NAME
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

class LiteKonanLibraryInfoProvider(customKonanHomeDir: String? = null) {

    private val konanHomeDir by lazy {
        customKonanHomeDir
            ?.takeIf { it.isNotEmpty() }
            ?.let { Paths.get(it) }
            ?.takeIf {
                // small sanity check to ensure that it's a valid Kotlin/Native home directory
                Files.isDirectory(it.resolve(KLIB_DIR_NAME))
            }
    }

    private val konanDataDir by lazy {
        val path = System.getenv("KONAN_DATA_DIR")?.let { Paths.get(it) }
            ?: Paths.get(System.getProperty("user.home"), ".konan")
        path.toAbsolutePath()
    }

    /**
     * Returns either [LiteKonanLibrary], or null if there is no such library in
     * Kotlin/Native distribution.
     */
    fun getDistributionLibraryInfo(libraryPath: File): LiteKonanLibrary? {
        // check whether it under Kotlin/Native root
        if (!isUnderKonanRoot(libraryPath))
            return null

        val manifestFile = libraryPath.resolve(KLIB_MANIFEST_FILE_NAME).takeIf { it.isFile } ?: return null
        val (platform: String?, dataDir: File) = getPlatformAndDataDir(libraryPath) ?: return null

        val manifestProperties = Properties().apply {
            try {
                manifestFile.inputStream().use { load(it) }
            } catch (e: IOException) {
                return null
            }
        }

        val name = manifestProperties[KLIB_PROPERTY_UNIQUE_NAME]?.toString() ?: return null
        val compilerVersion = manifestProperties[KLIB_PROPERTY_COMPILER_VERSION]?.toString() ?: return null

        val sourcePaths = if (name == KONAN_STDLIB_NAME) getStdlibSources(dataDir) else emptyList()

        return LiteKonanLibrary(libraryPath, sourcePaths, name, platform, compilerVersion)
    }

    private fun isUnderKonanRoot(libraryPath: File): Boolean {
        with(libraryPath.toPath()) {
            if (konanHomeDir != null && startsWith(konanHomeDir))
                return true

            return startsWith(konanDataDir)
        }
    }

    private fun getPlatformAndDataDir(libraryPath: File): Pair<String?, File>? {
        val parentDir = libraryPath.parentFile ?: return null
        val parentDirName = parentDir.name

        fun getDataDir(platformDir: File): File = platformDir.parentFile.parentFile

        return when (parentDirName) {
            KONAN_COMMON_LIBS_DIR_NAME -> null to getDataDir(parentDir)
            else -> {
                val grandParentDir = parentDir.parentFile ?: return null
                when {
                    grandParentDir.name == KONAN_PLATFORM_LIBS_DIR_NAME -> parentDirName to getDataDir(grandParentDir)
                    else -> return null
                }
            }
        }
    }

    private fun getStdlibSources(dataDir: File): Collection<File> {
        val sourcesDir = dataDir.resolve(KONAN_SOURCES_DIR_NAME).takeIf { it.isDirectory } ?: return emptyList()

        return sourcesDir.walkTopDown().maxDepth(1)
            .filter { it.isFile }
            .filter {
                with(it.name) { endsWith(".zip") && (startsWith("kotlin-stdlib") || startsWith("kotlin-test")) }
            }.toList()
    }
}
