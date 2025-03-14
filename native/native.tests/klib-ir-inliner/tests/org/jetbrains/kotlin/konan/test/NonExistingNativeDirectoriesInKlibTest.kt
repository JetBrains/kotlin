/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test

import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.konan.file.zipDirAs
import org.jetbrains.kotlin.konan.library.KLIB_TARGETS_FOLDER_NAME
import org.jetbrains.kotlin.konan.library.KonanLibrary
import org.jetbrains.kotlin.konan.library.impl.buildLibrary
import org.jetbrains.kotlin.konan.library.impl.createKonanLibrary
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.test.blackbox.support.util.mapToSet
import org.jetbrains.kotlin.library.KotlinAbiVersion
import org.jetbrains.kotlin.library.KotlinLibraryVersioning
import org.jetbrains.kotlin.library.SerializedMetadata
import org.jetbrains.kotlin.library.impl.KLIB_DEFAULT_COMPONENT_NAME
import org.jetbrains.kotlin.metadata.deserialization.MetadataVersion
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import org.jetbrains.kotlin.konan.file.File as KFile

class NonExistingNativeDirectoriesInKlibTest {
    @TempDir
    lateinit var tmpDir: Path

    @Test
    fun `no included dir`() {
        val includedFileNames = setOf("included1.txt", "included2.kt")

        val klibDir = writeLibrary(includedFileNames = includedFileNames)
        val klibFile = klibDir.compressKlib()

        assertTrue(klibDir.readLibrary().includedPaths.mapToSet { KFile(it).name } == includedFileNames)
        assertTrue(klibFile.readLibrary().includedPaths.mapToSet { KFile(it).name } == includedFileNames)

        klibDir.deleteNativeTargetSubdirectory("included")
        klibDir.compressKlib()

        assertTrue(klibDir.readLibrary().includedPaths.isEmpty())
        assertTrue(klibFile.readLibrary().includedPaths.isEmpty())
    }

    @Test
    fun `no native dir`() {
        val bitcodeFileNames = setOf("bitc0de.000", "btc.123")

        val klibDir = writeLibrary(bitcodeFileNames = bitcodeFileNames)
        val klibFile = klibDir.compressKlib()

        assertTrue(klibDir.readLibrary().bitcodePaths.mapToSet { KFile(it).name } == bitcodeFileNames)
        assertTrue(klibFile.readLibrary().bitcodePaths.mapToSet { KFile(it).name } == bitcodeFileNames)

        klibDir.deleteNativeTargetSubdirectory("native")
        klibDir.compressKlib()

        assertTrue(klibDir.readLibrary().bitcodePaths.isEmpty())
        assertTrue(klibFile.readLibrary().bitcodePaths.isEmpty())
    }

    companion object {
        private val TEST_TARGET = KonanTarget.LINUX_X64
        private const val TEST_MODULE_NAME = "non-existing-native-directories-test"
    }

    private fun writeLibrary(
        bitcodeFileNames: Collection<String> = emptyList(),
        includedFileNames: Collection<String> = emptyList(),
    ): KFile {
        fun createEmptyFile(name: String): String {
            val file = KFile(tmpDir.resolve(name))
            file.createNew()
            return file.absolutePath
        }

        val klibDir = KFile(tmpDir.resolve("uncompressed")).absoluteFile

        buildLibrary(
            natives = bitcodeFileNames.map(::createEmptyFile),
            included = includedFileNames.map(::createEmptyFile),
            linkDependencies = emptyList(),
            metadata = SerializedMetadata(byteArrayOf(), emptyList(), emptyList()),
            ir = null,
            versions = KotlinLibraryVersioning(
                abiVersion = KotlinAbiVersion.CURRENT, // does not matter
                compilerVersion = KotlinCompilerVersion.getVersion(), // does not matter
                metadataVersion = MetadataVersion.INSTANCE, // does not matter
            ),
            target = TEST_TARGET,
            output = klibDir.path,
            moduleName = TEST_MODULE_NAME,
            nopack = true,
            shortName = null,
            manifestProperties = null,
        )

        return klibDir
    }

    private fun KFile.readLibrary(): KonanLibrary =
        createKonanLibrary(libraryFilePossiblyDenormalized = this, component = KLIB_DEFAULT_COMPONENT_NAME, target = TEST_TARGET)

    private fun KFile.compressKlib(): KFile {
        val klibFile = this.parentFile.child("compressed.klib")
        if (klibFile.exists) klibFile.deleteRecursively()
        this.zipDirAs(klibFile)
        return klibFile
    }

    private fun KFile.deleteNativeTargetSubdirectory(subdirectoryName: String) {
        val subdirectory = child(KLIB_DEFAULT_COMPONENT_NAME)
            .child(KLIB_TARGETS_FOLDER_NAME)
            .child(TEST_TARGET.visibleName)
            .child(subdirectoryName)
        assertTrue(subdirectory.isDirectory) { "Directory does not exist: $subdirectory" }
        subdirectory.deleteRecursively()
    }
}
