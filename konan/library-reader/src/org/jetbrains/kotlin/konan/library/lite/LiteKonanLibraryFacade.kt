/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.library.lite

import org.jetbrains.kotlin.konan.library.*
import java.io.File
import java.io.IOException
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*

// TODO: agree how to distinguish between Native and non-Native (ex: JS) KLIBs
object LiteKonanLibraryFacade {
    /**
     * Use this [LiteKonanLibraryProvider] to get information about any Kotlin/Native library.
     */
    fun getLibraryProvider(): LiteKonanLibraryProvider = DefaultLiteKonanLibraryProvider

    /**
     * Use this [LiteKonanLibraryProvider] to get information about libraries inside of Kotlin/Native distribution.
     */
    fun getDistributionLibraryProvider(customKonanHomeDir: File?): LiteKonanLibraryProvider =
        FromDistributionLiteKonanLibraryProvider(customKonanHomeDir)
}

interface LiteKonanLibraryProvider {
    /**
     * Returns either [LiteKonanLibrary], or null if path does not point to a valid Kotlin/Native library
     * or the current [LiteKonanLibraryProvider] does not accept the library due to internal restrictions.
     */
    fun getLibrary(libraryPath: File): LiteKonanLibrary?
}

private object DefaultLiteKonanLibraryProvider : LiteKonanLibraryProvider {
    override fun getLibrary(libraryPath: File): LiteKonanLibraryImpl? {
        val manifest = loadManifest(libraryPath) ?: return null

        val name = manifest.getProperty(KLIB_PROPERTY_UNIQUE_NAME) ?: return null
        val compilerVersion = manifest.getProperty(KLIB_PROPERTY_COMPILER_VERSION) ?: return null

        return LiteKonanLibraryImpl(
            path = libraryPath,
            name = name,
            compilerVersion = compilerVersion
        )
    }

    private fun loadManifest(libraryPath: File): Properties? {
        val manifestFile = libraryPath.resolve(KLIB_MANIFEST_FILE_NAME).takeIf { it.isFile } ?: return null

        return try {
            manifestFile.inputStream().use { Properties().apply { load(it) } }
        } catch (_: IOException) {
            return null
        }
    }
}

private class FromDistributionLiteKonanLibraryProvider(customKonanHomeDir: File?) : LiteKonanLibraryProvider {
    private val konanHomeDir: Path? by lazy {
        customKonanHomeDir?.takeIf {
            // small sanity check to ensure that it's a valid Kotlin/Native home directory
            customKonanHomeDir.resolve(KLIB_DIR_NAME).isDirectory
        }?.toPath()
    }

    private val konanDataDir: Path by lazy {
        val path = System.getenv("KONAN_DATA_DIR")?.let { Paths.get(it) }
            ?: Paths.get(System.getProperty("user.home"), ".konan")
        path.toAbsolutePath()
    }

    override fun getLibrary(libraryPath: File): LiteKonanLibrary? {
        // check whether it under Kotlin/Native root
        if (!isUnderKonanRoot(libraryPath)) return null

        val library = DefaultLiteKonanLibraryProvider.getLibrary(libraryPath) ?: return null

        val (platform: String?, dataDir: File) = getPlatformAndDataDir(libraryPath) ?: return null
        library.platform = platform

        if (library.name == KONAN_STDLIB_NAME)
            library.sourcePaths = getStdlibSources(dataDir)

        return library
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

internal class LiteKonanLibraryImpl(
    override val path: File,
    override val name: String,
    internal val compilerVersion: String
) : LiteKonanLibrary {
    override var platform: String? = null
    override var sourcePaths: Collection<File> = emptyList()
}
