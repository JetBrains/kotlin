/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test

import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.konan.file.zipDirAs
import org.jetbrains.kotlin.konan.library.components.KlibBitcodeConstants.KLIB_BITCODE_FOLDER_NAME
import org.jetbrains.kotlin.konan.library.components.KlibNativeConstants.KLIB_TARGETS_FOLDER_NAME
import org.jetbrains.kotlin.konan.library.components.KlibNativeIncludedBinariesConstants.KLIB_NATIVE_INCLUDED_BINARIES_FOLDER_NAME
import org.jetbrains.kotlin.konan.library.components.bitcode
import org.jetbrains.kotlin.konan.library.components.nativeIncludedBinaries
import org.jetbrains.kotlin.konan.library.impl.buildLibrary
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.test.blackbox.support.util.mapToSet
import org.jetbrains.kotlin.library.KotlinAbiVersion
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.KotlinLibraryVersioning
import org.jetbrains.kotlin.library.SerializedMetadata
import org.jetbrains.kotlin.library.impl.KLIB_DEFAULT_COMPONENT_NAME
import org.jetbrains.kotlin.library.loader.KlibLoader
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
    fun `no Native included binaries dir`() {
        val nativeIncludedBinaryFileNames = setOf("included1.txt", "included2.kt")

        val klibDir = writeLibrary(includedBinaryFileNames = nativeIncludedBinaryFileNames)
        val klibFile = klibDir.compressKlib()

        assertTrue(klibDir.readLibrary().nativeIncludedBinaries(TEST_TARGET)?.nativeIncludedBinaryFilePaths?.mapToSet { KFile(it).name } == nativeIncludedBinaryFileNames)
        assertTrue(klibFile.readLibrary().nativeIncludedBinaries(TEST_TARGET)?.nativeIncludedBinaryFilePaths?.mapToSet { KFile(it).name } == nativeIncludedBinaryFileNames)

        klibDir.deleteNativeTargetSubdirectory(KLIB_NATIVE_INCLUDED_BINARIES_FOLDER_NAME)
        klibDir.compressKlib()

        assertTrue(klibDir.readLibrary().nativeIncludedBinaries(TEST_TARGET)?.nativeIncludedBinaryFilePaths.isNullOrEmpty())
        assertTrue(klibFile.readLibrary().nativeIncludedBinaries(TEST_TARGET)?.nativeIncludedBinaryFilePaths.isNullOrEmpty())
    }

    @Test
    fun `no bitcode dir`() {
        val bitcodeFileNames = setOf("bitc0de.000", "btc.123")

        val klibDir = writeLibrary(bitcodeFileNames = bitcodeFileNames)
        val klibFile = klibDir.compressKlib()

        assertTrue(klibDir.readLibrary().bitcode(TEST_TARGET)?.bitcodeFilePaths?.mapToSet { KFile(it).name } == bitcodeFileNames)
        assertTrue(klibFile.readLibrary().bitcode(TEST_TARGET)?.bitcodeFilePaths?.mapToSet { KFile(it).name } == bitcodeFileNames)

        klibDir.deleteNativeTargetSubdirectory(KLIB_BITCODE_FOLDER_NAME)
        klibDir.compressKlib()

        assertTrue(klibDir.readLibrary().bitcode(TEST_TARGET)?.bitcodeFilePaths.isNullOrEmpty())
        assertTrue(klibFile.readLibrary().bitcode(TEST_TARGET)?.bitcodeFilePaths.isNullOrEmpty())
    }

    companion object {
        private val TEST_TARGET = KonanTarget.LINUX_X64
        private const val TEST_MODULE_NAME = "non-existing-native-directories-test"
    }

    private fun writeLibrary(
        bitcodeFileNames: Collection<String> = emptyList(),
        includedBinaryFileNames: Collection<String> = emptyList(),
    ): KFile {
        fun createEmptyFile(name: String): String {
            val file = KFile(tmpDir.resolve(name))
            file.createNew()
            return file.absolutePath
        }

        val klibDir = KFile(tmpDir.resolve("uncompressed")).absoluteFile

        buildLibrary(
            natives = bitcodeFileNames.map(::createEmptyFile),
            included = includedBinaryFileNames.map(::createEmptyFile),
            linkDependencies = emptyList(),
            metadata = SerializedMetadata(byteArrayOf(), emptyList(), emptyList(), MetadataVersion.INSTANCE.toArray()),
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

    private fun KFile.readLibrary(): KotlinLibrary =
        KlibLoader { libraryPaths(this@readLibrary.path) }.load().librariesStdlibFirst.single()

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
