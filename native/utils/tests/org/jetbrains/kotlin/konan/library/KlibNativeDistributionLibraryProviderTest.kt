/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.library

import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.library.KlibMockDSL.Companion.generateRandomName
import org.jetbrains.kotlin.library.KlibMockDSL.Companion.mockKlib
import org.jetbrains.kotlin.library.KotlinAbiVersion
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.KotlinLibraryVersioning
import org.jetbrains.kotlin.library.impl.BuiltInsPlatform
import org.jetbrains.kotlin.library.loader.KlibLoader
import org.jetbrains.kotlin.library.loader.reportLoadingProblemsIfAny
import org.jetbrains.kotlin.library.manifest
import org.jetbrains.kotlin.metadata.deserialization.MetadataVersion
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.Properties
import kotlin.io.path.createDirectories
import kotlin.io.path.pathString
import kotlin.io.path.writeText

class KlibNativeDistributionLibraryProviderTest {
    @TempDir
    private lateinit var tempDir: Path

    @Test
    fun `Loading stdlib or platform libs`() {
        val nativeHome = emulateNativeDistribution(numberOfPlatformLibs = 20)

        assertEquals(0, loadLibs(nativeHome).size)
        assertEquals(1, loadLibs(nativeHome, loadStdlib = true).size)
        assertEquals(20, loadLibs(nativeHome, loadPlatformLibs = true).size)
        assertEquals(21, loadLibs(nativeHome, loadStdlib = true, loadPlatformLibs = true).size)
    }

    @Test
    fun `Loading platform libs without occasional system files in the Native distro`() {
        testLoadingPlatformLibsWithUnexpectedSystemFiles(addOccasionalSystemFiles = false)
    }

    @Test
    fun `Loading platform libs with occasional system files in the Native distro`() {
        testLoadingPlatformLibsWithUnexpectedSystemFiles(addOccasionalSystemFiles = true)
    }

    private fun testLoadingPlatformLibsWithUnexpectedSystemFiles(addOccasionalSystemFiles: Boolean) {
        val libraries = loadLibs(
            nativeHome = emulateNativeDistribution(numberOfPlatformLibs = 3, addOccasionalSystemFiles = addOccasionalSystemFiles),
            loadPlatformLibs = true,
        )
        assertEquals(3, libraries.size)
    }

    @Test
    fun `Loading libs with isImplicitlyLoadedFromKotlinNativeDistribution flag`() {
        val nativeHome = emulateNativeDistribution(numberOfPlatformLibs = 3)

        val customKlib = tempDir.createMockKlib(generateRandomName(10))

        val libraries1 = loadLibs(
            nativeHome = nativeHome,
            loadStdlib = true,
            loadPlatformLibs = true,
            customLibraryPaths = listOf(customKlib)
        )
        assertEquals(5, libraries1.size)
        for (library in libraries1) {
            val libraryShouldNotBeMarkedAsImplicitlyLoaded = library.path == customKlib
            assertEquals(libraryShouldNotBeMarkedAsImplicitlyLoaded, !library.isImplicitlyLoadedFromKotlinNativeDistribution)
        }

        val stdlib = libraries1.first().path
        val somePlatformLib = libraries1.last().path

        val libraries2 = loadLibs(
            nativeHome = nativeHome,
            loadStdlib = true,
            loadPlatformLibs = true,
            customLibraryPaths = listOf(customKlib, stdlib, somePlatformLib)
        )
        assertEquals(5, libraries2.size)
        for (library in libraries2) {
            val libraryShouldNotBeMarkedAsImplicitlyLoaded =
                library.path == customKlib || library.path == stdlib || library.path == somePlatformLib
            assertEquals(libraryShouldNotBeMarkedAsImplicitlyLoaded, !library.isImplicitlyLoadedFromKotlinNativeDistribution)
        }
    }

    private fun loadLibs(
        nativeHome: Path,
        loadStdlib: Boolean = false,
        loadPlatformLibs: Boolean = false,
        customLibraryPaths: List<Path> = emptyList(),
    ): List<KotlinLibrary> {
        val result = KlibLoader {
            libraryProviders(
                KlibNativeDistributionLibraryProvider(nativeHome.toFile()) {
                    if (loadStdlib) withStdlib()
                    if (loadPlatformLibs) withPlatformLibs(TEST_TARGET)
                }
            )
            libraryPaths(customLibraryPaths.map { it.pathString })
        }.load()
        result.reportLoadingProblemsIfAny { _, message -> fail(message) }
        return result.librariesStdlibFirst
    }

    private fun emulateNativeDistribution(
        numberOfPlatformLibs: Int,
        addOccasionalSystemFiles: Boolean = false,
    ): Path {
        require(numberOfPlatformLibs >= 0)

        val distDir = tempDir.resolve("kotlin-native-dist")

        distDir.resolve("klib/common").createMockKlib("stdlib")

        with(distDir.resolve("klib/platform/${TEST_TARGET.name}")) {
            createDirectories()
            generateSequence('a') { it + 1 }.take(numberOfPlatformLibs).forEach { shortName ->
                createMockKlib("org.jetbrains.kotlin.native.platform.$shortName")
            }

            if (addOccasionalSystemFiles) {
                for (fileName in listOf(".DS_Store", "Desktop.ini")) {
                    resolve("$fileName.dir").createDirectories()
                    resolve("$fileName.file").writeText("")
                }
            }
        }

        return distDir
    }

    private fun Path.createMockKlib(name: String, customManifestProperties: Properties.() -> Unit = {}): Path =
        mockKlib(resolve(name)) {
            manifest(
                uniqueName = name,
                builtInsPlatform = BuiltInsPlatform.NATIVE,
                versioning = KotlinLibraryVersioning(
                    compilerVersion = null,
                    abiVersion = KotlinAbiVersion.CURRENT,
                    metadataVersion = MetadataVersion.INSTANCE,
                ),
                other = customManifestProperties
            )
        }

    companion object {
        private val TEST_TARGET = KonanTarget.MACOS_ARM64
    }
}
