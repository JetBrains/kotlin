/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.klib

import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.io.zipDirAs
import org.jetbrains.kotlin.konan.library.components.KlibBitcodeConstants.KLIB_BITCODE_FOLDER_NAME
import org.jetbrains.kotlin.library.components.KlibNativeConstants.KLIB_TARGETS_FOLDER_NAME
import org.jetbrains.kotlin.konan.library.components.KlibNativeIncludedBinariesConstants.KLIB_NATIVE_INCLUDED_BINARIES_FOLDER_NAME
import org.jetbrains.kotlin.konan.library.components.bitcode
import org.jetbrains.kotlin.konan.library.components.nativeIncludedBinaries
import org.jetbrains.kotlin.konan.library.writer.includeBitcode
import org.jetbrains.kotlin.konan.library.writer.includeNativeIncludedBinaries
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.test.blackbox.support.util.mapToSet
import org.jetbrains.kotlin.library.KlibConstants.KLIB_DEFAULT_COMPONENT_NAME
import org.jetbrains.kotlin.library.KotlinAbiVersion
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.KotlinLibraryVersioning
import org.jetbrains.kotlin.library.SerializedMetadata
import org.jetbrains.kotlin.library.impl.BuiltInsPlatform
import org.jetbrains.kotlin.library.loader.KlibLoader
import org.jetbrains.kotlin.library.writer.KlibWriter
import org.jetbrains.kotlin.library.writer.includeMetadata
import org.jetbrains.kotlin.metadata.deserialization.MetadataVersion
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.absolute
import kotlin.io.path.createFile
import kotlin.io.path.deleteRecursively
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.name

class NonExistingNativeDirectoriesInKlibTest {
    @TempDir
    lateinit var tmpDir: Path

    @Test
    fun `no Native included binaries dir`() {
        val nativeIncludedBinaryFileNames = setOf("included1.txt", "included2.kt")

        val klibDir = writeLibrary(includedBinaryFileNames = nativeIncludedBinaryFileNames)
        val klibFile = klibDir.compressKlib()

        assertTrue(klibDir.readLibrary().nativeIncludedBinaries(TEST_TARGET)?.nativeIncludedBinaryFilePaths?.mapToSet { it.name } == nativeIncludedBinaryFileNames)
        assertTrue(klibFile.readLibrary().nativeIncludedBinaries(TEST_TARGET)?.nativeIncludedBinaryFilePaths?.mapToSet { it.name } == nativeIncludedBinaryFileNames)

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

        assertTrue(klibDir.readLibrary().bitcode(TEST_TARGET)?.bitcodeFilePaths?.mapToSet { it.name } == bitcodeFileNames)
        assertTrue(klibFile.readLibrary().bitcode(TEST_TARGET)?.bitcodeFilePaths?.mapToSet { it.name } == bitcodeFileNames)

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
    ): Path {
        fun createEmptyFile(name: String): Path = tmpDir.resolve(name).apply(Path::createFile)

        val klibDir = tmpDir.resolve("uncompressed").absolute()

        KlibWriter {
            manifest {
                moduleName(TEST_MODULE_NAME)
                versions(
                    KotlinLibraryVersioning(
                        abiVersion = KotlinAbiVersion.CURRENT, // does not matter
                        compilerVersion = KotlinCompilerVersion.getVersion(), // does not matter
                        metadataVersion = MetadataVersion.INSTANCE, // does not matter
                    )
                )
                platformAndTargets(BuiltInsPlatform.NATIVE, TEST_TARGET.name)
            }
            includeMetadata(SerializedMetadata(byteArrayOf(), emptyList(), emptyList(), MetadataVersion.INSTANCE.toArray()))
            includeBitcode(TEST_TARGET, bitcodeFileNames.map(::createEmptyFile))
            includeNativeIncludedBinaries(TEST_TARGET, includedBinaryFileNames.map(::createEmptyFile))
        }.writeTo(klibDir)

        return klibDir
    }

    private fun Path.readLibrary(): KotlinLibrary =
        KlibLoader { libraryPaths(this@readLibrary) }.load().librariesStdlibFirst.single()

    @OptIn(ExperimentalPathApi::class)
    private fun Path.compressKlib(): Path {
        val klibFile = this.parent.resolve("compressed.klib")
        if (klibFile.exists()) klibFile.deleteRecursively()
        this.zipDirAs(klibFile)
        return klibFile
    }

    @OptIn(ExperimentalPathApi::class)
    private fun Path.deleteNativeTargetSubdirectory(subdirectoryName: String) {
        val subdirectory = resolve(KLIB_DEFAULT_COMPONENT_NAME)
            .resolve(KLIB_TARGETS_FOLDER_NAME)
            .resolve(TEST_TARGET.visibleName)
            .resolve(subdirectoryName)
        assertTrue(subdirectory.isDirectory()) { "Directory does not exist: $subdirectory" }
        subdirectory.deleteRecursively()
    }
}
