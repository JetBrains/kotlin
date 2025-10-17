/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native.interop.gen

import org.jetbrains.kotlin.config.KlibAbiCompatibilityLevel
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.konan.file.unzipTo
import org.jetbrains.kotlin.konan.library.KLIB_TARGETS_FOLDER_NAME
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.library.KLIB_PROPERTY_DEPENDS
import org.jetbrains.kotlin.library.KLIB_PROPERTY_NATIVE_TARGETS
import org.jetbrains.kotlin.library.KLIB_PROPERTY_SHORT_NAME
import org.jetbrains.kotlin.library.KlibFileSystemDiff
import org.jetbrains.kotlin.library.KlibMockDSL
import org.jetbrains.kotlin.library.KotlinLibraryVersioning
import org.jetbrains.kotlin.library.SerializedMetadata
import org.jetbrains.kotlin.library.impl.BuiltInsPlatform
import org.jetbrains.kotlin.library.loader.KlibLoader
import org.jetbrains.kotlin.library.manifest
import org.jetbrains.kotlin.library.metadata
import org.jetbrains.kotlin.library.resources
import org.jetbrains.kotlin.native.interop.gen.jvm.createInteropLibrary
import org.jetbrains.kotlin.util.toCInteropKlibMetadataVersion
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.fail
import java.io.File
import java.util.Properties
import java.util.UUID
import kotlin.collections.forEach
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import org.jetbrains.kotlin.konan.file.File as KlibFile

class CInteropKlibWritingTest {
    @Rule
    @JvmField
    val testFilesFactory = TestFilesFactory()

    private lateinit var tmpDir: File

    @BeforeTest
    fun setUp() {
        tmpDir = testFilesFactory.tempFiles().directory
    }

    @AfterTest
    fun tearDown() {
        tmpDir.deleteRecursively()
    }

    @Test
    fun `Writing C-interop klib with different unique names`() {
        listOf("foo", "bar-bar", "something.Different.X64").forEach { uniqueName ->
            Parameters(uniqueName = uniqueName).doTest()
        }
    }

    @Test
    fun `Writing C-interop klib with different short names`() {
        listOf("foo", "bar-bar", null).forEach { shortName ->
            Parameters(shortName = shortName).doTest()
        }
    }

    @Test
    fun `Writing C-interop klib with different targets`() {
        listOf(KonanTarget.IOS_ARM64, KonanTarget.MACOS_ARM64, KonanTarget.WATCHOS_ARM64).forEach { target ->
            Parameters(target = target).doTest()
        }
    }

    @Test
    fun `Writing C-interop klib with different ABI levels`() {
        KlibAbiCompatibilityLevel.entries.forEach { abiLevel ->
            Parameters(abiLevel = abiLevel).doTest()
        }
    }

    @Test
    fun `Writing C-interop klib with custom manifest properties`() {
        Parameters(customManifestProperties = listOf("key1" to "value1", "key2" to "value2 value3")).doTest()
    }

    @Test
    fun `Writing C-interop klib with custom included files`() {
        val random = Random(System.nanoTime())
        var index = 0

        fun newIncludedFile() = BinaryFile(tmpDir.resolve("file${index++}.o"), random.nextBytes(100))

        repeat(3) {
            Parameters(includedFiles = List(index + 1) { newIncludedFile() }).doTest()
        }
    }

    @Test
    fun `Writing C-interop klib with custom bitcode files`() {
        val random = Random(System.nanoTime())
        var index = 0

        fun newBitCodeFile() = BinaryFile(tmpDir.resolve("file${index++}.bc"), random.nextBytes(100))

        repeat(3) {
            Parameters(bitCodeFiles = List(index + 1) { newBitCodeFile() }).doTest()
        }
    }

    @Test
    fun `Writing C-interop klib packed and unpacked`() {
        Parameters(nopack = false).doTest()
        Parameters(nopack = true).doTest()
    }

    @Test
    fun `Writing C-interop klib with dependencies`() {
        fun mockDependency(name: String): Dependency = Dependency(
                name = name,
                path = mockKlib {
                    manifest(uniqueName = name, builtInsPlatform = BuiltInsPlatform.NATIVE, versioning = KotlinLibraryVersioning(null, null, null))
                }.path
        )

        Parameters(dependencies = listOf(mockDependency("dep1"), mockDependency("dep2"))).doTest()
    }

    private fun Parameters.doTest() {
        val cinteropKlib = cinteropKlib()

        val mockKlib = mockKlib {
            metadata(metadata)
            targets(target) {
                included(includedFiles)
                native(bitCodeFiles)
            }
            resources()
            manifest(
                    uniqueName = uniqueName,
                    builtInsPlatform = BuiltInsPlatform.NATIVE,
                    versioning = KotlinLibraryVersioning(
                            compilerVersion = KotlinCompilerVersion.VERSION, // always fixed
                            abiVersion = abiLevel.toAbiVersionForManifest(), // derived from KlibAbiCompatibilityLevel
                            metadataVersion = abiLevel.toCInteropKlibMetadataVersion(), // derived from KlibAbiCompatibilityLevel
                    ),
                    other = {
                        this[KLIB_PROPERTY_NATIVE_TARGETS] = target.name
                        if (shortName != null) this[KLIB_PROPERTY_SHORT_NAME] = shortName
                        if (dependencies.isNotEmpty()) this[KLIB_PROPERTY_DEPENDS] = dependencies.joinToString(" ") { it.name }
                        customManifestProperties.forEach { (key, value) -> this[key] = value }
                    }
            )
        }

        val result = KlibFileSystemDiff(mockKlib, cinteropKlib).recursiveDiff()
        if (result !is KlibFileSystemDiff.Result.Identical) {
            fail(result.toString())
        }
    }

    private class Parameters(
            val target: KonanTarget = KonanTarget.IOS_ARM64,
            val uniqueName: String = "foo",
            val shortName: String? = null,
            val abiLevel: KlibAbiCompatibilityLevel = KlibAbiCompatibilityLevel.LATEST_STABLE,
            val customManifestProperties: List<Pair<String, String>> = emptyList(),
            val bitCodeFiles: List<BinaryFile> = emptyList(),
            val includedFiles: List<BinaryFile> = emptyList(),
            val nopack: Boolean = true,
            val dependencies: List<Dependency> = emptyList(),
    ) {
        val metadata: SerializedMetadata = KlibMockDSL.generateRandomMetadata() // each time generate some random fake metadata
    }

    private fun mockKlib(init: KlibMockDSL.() -> Unit): File {
        return KlibMockDSL.mockKlib(createNewKlibDir(), init)
    }

    private fun Parameters.cinteropKlib(): File {
        val klibDir = createNewKlibDir()
        val klibOutput = (if (nopack) klibDir else klibDir.resolveSibling(klibDir.nameWithoutExtension + ".klib")).path

        createInteropLibrary(
                serializedMetadata = metadata,
                outputPath = klibOutput,
                moduleName = uniqueName,
                nativeBitcodeFiles = bitCodeFiles.map { it.path },
                target = target,
                manifest = Properties().apply {
                    customManifestProperties.forEach { (key, value) -> setProperty(key, value) }
                },
                dependencies = KlibLoader { libraryPaths(dependencies.map { it.path }) }.load().librariesStdlibFirst,
                nopack = nopack,
                shortName = shortName,
                staticLibraries = includedFiles.map { it.path },
                klibAbiCompatibilityLevel = abiLevel,
        )

        if (!nopack) {
            KlibFile(klibOutput).unzipTo(KlibFile(klibDir.path))
        }

        return klibDir
    }

    private fun createNewKlibDir(): File = tmpDir.resolve(UUID.randomUUID().toString()).apply(File::mkdirs)
}

private fun KlibMockDSL.targets(target: KonanTarget, init: KlibMockDSL.() -> Unit = {}) {
    dir(KLIB_TARGETS_FOLDER_NAME) {
        dir(target.name, init)
    }
}

private class BinaryFile(val file: File, content: ByteArray) {
    init {
        file.writeBytes(content)
    }

    val name: String get() = file.name
    val path: String get() = file.path
    val content: ByteArray get() = file.readBytes()
}

private fun KlibMockDSL.included(files: List<BinaryFile> = emptyList()) {
    dir("included") {
        files.forEach { file -> file(file.name, file.content) }
    }
}

private fun KlibMockDSL.native(files: List<BinaryFile> = emptyList()) {
    dir("native") {
        files.forEach { file -> file(file.name, file.content) }
    }
}

private class Dependency(val name: String, val path: String)
