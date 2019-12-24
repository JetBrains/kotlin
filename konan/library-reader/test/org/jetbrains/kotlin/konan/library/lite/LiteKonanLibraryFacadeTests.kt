/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.library.lite

import org.jetbrains.kotlin.konan.library.KONAN_DISTRIBUTION_KLIB_DIR
import org.jetbrains.kotlin.konan.library.KONAN_DISTRIBUTION_COMMON_LIBS_DIR
import org.jetbrains.kotlin.konan.library.KONAN_DISTRIBUTION_PLATFORM_LIBS_DIR
import org.jetbrains.kotlin.konan.library.KONAN_STDLIB_NAME
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.io.FileFilter
import java.nio.file.Files
import java.util.zip.ZipException
import java.util.zip.ZipFile

class LiteKonanLibraryFacadeTests {

    @Test
    fun allLibrariesRecognized() {
        val libraryProvider = LiteKonanLibraryFacade.getLibraryProvider()

        val actualLibraries = collectLibrariesFromLocalFS(libraryProvider)
        val expectedLibraries = librariesExpectedInDistribution() + librariesExpectedInExternalDir()

        compareLibraries(
            expectedLibraries,
            actualLibraries,
            /* `LiteKonanLibraryFacade.getLibraryProvider()` is currently platform-agnostic */ true
        )
    }

    @Test
    fun onlyFromDistributionLibrariesRecognized() {
        val libraryProvider = LiteKonanLibraryFacade.getDistributionLibraryProvider(konanHomeDir)

        val actualLibraries = collectLibrariesFromLocalFS(libraryProvider)
        val expectedLibraries = librariesExpectedInDistribution()

        compareLibraries(expectedLibraries, actualLibraries, false)
    }

    @Test
    fun sourcesAttachedToStdlib() {
        val libraryProvider = LiteKonanLibraryFacade.getDistributionLibraryProvider(konanHomeDir)

        val stdlib = collectLibrariesFromLocalFS(libraryProvider).values.firstOrNull { it.name == KONAN_STDLIB_NAME }
            ?: error("Could not load Kotlin/Native $KONAN_STDLIB_NAME from $konanHomeDir")

        assertTrue("Kotlin/Native $KONAN_STDLIB_NAME sources must not be empty", stdlib.sourcePaths.isNotEmpty())

        assertTrue(
            "Each path in $KONAN_STDLIB_NAME.sourcePaths must be either non-empty directory or ZIP (JAR) file",
            stdlib.sourcePaths.all { sourcePath ->
                when {
                    sourcePath.isDirectory -> Files.newDirectoryStream(sourcePath.toPath()).use { it.any { true } }
                    sourcePath.isFile -> try {
                        ZipFile(sourcePath).use { it.name != null }
                    } catch (e: ZipException) {
                        false
                    }
                    else -> false
                }
            })

    }

    private fun getPotentialLibraryPathsOnLocalFS(): List<File> {
        val roots = mutableListOf(externalLibsDir)

        roots += klibDir.resolve(KONAN_DISTRIBUTION_COMMON_LIBS_DIR)
        roots += klibDir.resolve(KONAN_DISTRIBUTION_PLATFORM_LIBS_DIR).listFiles(FileFilter { it.isDirectory })?.toList() ?: emptyList()

        return roots.flatMap { it.listFiles()?.toList() ?: emptyList() }
    }

    private fun collectLibrariesFromLocalFS(libraryProvider: LiteKonanLibraryProvider): Map<File, LiteKonanLibrary> =
        getPotentialLibraryPathsOnLocalFS().mapNotNull { libraryProvider.getLibrary(it) }.toLibraryMap()

    private fun librariesExpectedInDistribution(): Map<File, FakeLibraryForTest> = listOf(
        FakeLibraryForTest(klibDir.resolve(KONAN_DISTRIBUTION_COMMON_LIBS_DIR, KONAN_STDLIB_NAME)),
        FakeLibraryForTest(klibDir.resolve(KONAN_DISTRIBUTION_PLATFORM_LIBS_DIR, "macos_x64", "foo"), platform = "macos_x64"),
        FakeLibraryForTest(klibDir.resolve(KONAN_DISTRIBUTION_PLATFORM_LIBS_DIR, "macos_x64", "bar"), platform = "macos_x64"),
        FakeLibraryForTest(klibDir.resolve(KONAN_DISTRIBUTION_PLATFORM_LIBS_DIR, "macos_x64", "baz"), platform = "macos_x64")
    ).toLibraryMap()

    private fun librariesExpectedInExternalDir(): Map<File, FakeLibraryForTest> = listOf(
        FakeLibraryForTest(externalLibsDir.resolve("correct"))
    ).toLibraryMap()

    private fun compareLibraries(expected: Map<File, FakeLibraryForTest>, actual: Map<File, LiteKonanLibrary>, ignorePlatform: Boolean) {
        assertEquals("Libraries paths mismatch", expected.keys, actual.keys)

        for (key in expected.keys) {
            val expectedLibrary = expected.getValue(key)
            val actualLibrary = actual.getValue(key)

            assertEquals("Library path mismatch", expectedLibrary.path, actualLibrary.path)

            if (!ignorePlatform) {
                assertEquals("Library platform mismatch, path=${expectedLibrary.path}", expectedLibrary.platform, actualLibrary.platform)
            }
        }
    }

    private companion object {
        val klibDir = konanHomeDir.resolve(KONAN_DISTRIBUTION_KLIB_DIR)
        val externalLibsDir = testDataDir.resolve("external-libs")

        fun <T : LiteKonanLibrary> List<T>.toLibraryMap() = map { it.path to it }.toMap()
    }
}

private class FakeLibraryForTest(
    override val path: File,
    override val platform: String? = null
) : LiteKonanLibrary {
    override val sourcePaths: Collection<File> = emptyList()
    override val name: String = path.name
}
