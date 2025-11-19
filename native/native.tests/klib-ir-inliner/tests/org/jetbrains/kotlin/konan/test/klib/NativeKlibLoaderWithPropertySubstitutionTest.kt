/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.klib

import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.konan.library.impl.createKonanLibrary
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.util.KlibNativeManifestTransformer
import org.jetbrains.kotlin.library.*
import org.jetbrains.kotlin.library.KlibMockDSL.Companion.generateRandomMetadata
import org.jetbrains.kotlin.library.KlibMockDSL.Companion.generateRandomName
import org.jetbrains.kotlin.library.KlibMockDSL.Companion.mockKlib
import org.jetbrains.kotlin.library.impl.BuiltInsPlatform
import org.jetbrains.kotlin.library.impl.KLIB_DEFAULT_COMPONENT_NAME
import org.jetbrains.kotlin.library.loader.KlibLoader
import org.jetbrains.kotlin.metadata.deserialization.MetadataVersion
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.*
import org.jetbrains.kotlin.konan.file.File as KlibFile

class NativeKlibLoaderWithPropertySubstitutionTest {
    @TempDir
    lateinit var tmpDir: File

    @Test
    fun `Library is loaded without property substitution`() {
        val klibPath = generateKlib()

        val manifestOfNativeKlib = loadManifestOfNativeKlib(klibPath, target = null)
        manifestOfNativeKlib.assertContainsAllProperties(CUSTOM_MANIFEST_PROPERTIES)

        val manifestOfNativeKlib2 = loadManifestOfNativeKlib(klibPath, target = KonanTarget.MACOS_X64) // irrelevant target
        manifestOfNativeKlib2.assertContainsAllProperties(CUSTOM_MANIFEST_PROPERTIES)
        assertEquals(manifestOfNativeKlib, manifestOfNativeKlib2)

        val manifestOfLegacyNativeKlib = loadManifestOfLegacyNativeKlib(klibPath, target = null)
        manifestOfLegacyNativeKlib.assertContainsAllProperties(CUSTOM_MANIFEST_PROPERTIES)
        assertEquals(manifestOfNativeKlib, manifestOfLegacyNativeKlib)

        val manifestOfLegacyNativeKlib2 = loadManifestOfLegacyNativeKlib(klibPath, target = KonanTarget.MACOS_X64) // irrelevant target
        manifestOfLegacyNativeKlib2.assertContainsAllProperties(CUSTOM_MANIFEST_PROPERTIES)
        assertEquals(manifestOfNativeKlib, manifestOfLegacyNativeKlib2)
    }

    @Test
    fun `Library is loaded with property substitution`() {
        val klibPath = generateKlib()

        val manifestOfNativeKlib = loadManifestOfNativeKlib(klibPath, target = KonanTarget.LINUX_ARM64)
        manifestOfNativeKlib.assertContainsAllProperties(CUSTOM_MANIFEST_PROPERTIES - BASE_SUBSTITUTED_PROPERTY)
        manifestOfNativeKlib.assertContainsAllProperties(mapOf(BASE_SUBSTITUTED_PROPERTY to RESULTING_SUBSTITUTED_PROPERTY_VALUE))

        val manifestOfLegacyNativeKlib = loadManifestOfLegacyNativeKlib(klibPath, target = KonanTarget.LINUX_ARM64)
        manifestOfLegacyNativeKlib.assertContainsAllProperties(CUSTOM_MANIFEST_PROPERTIES - BASE_SUBSTITUTED_PROPERTY)
        manifestOfLegacyNativeKlib.assertContainsAllProperties(mapOf(BASE_SUBSTITUTED_PROPERTY to RESULTING_SUBSTITUTED_PROPERTY_VALUE))

        assertEquals(manifestOfLegacyNativeKlib, manifestOfNativeKlib)
    }

    private fun generateKlib(): String = mockKlib(tmpDir.resolve(generateRandomName(10))) {
        metadata(generateRandomMetadata())
        resources()
        manifest(
            uniqueName = generateRandomName(20),
            builtInsPlatform = BuiltInsPlatform.NATIVE,
            versioning = KotlinLibraryVersioning(
                compilerVersion = KotlinCompilerVersion.getVersion(),
                abiVersion = KotlinAbiVersion.CURRENT,
                metadataVersion = MetadataVersion.INSTANCE,
            ),
            other = {
                // add custom properties to manifest
                this += CUSTOM_MANIFEST_PROPERTIES
            }
        )
    }.path

    private fun loadManifestOfLegacyNativeKlib(klibPath: String, target: KonanTarget?): Properties {
        return createKonanLibrary(
            libraryFilePossiblyDenormalized = KlibFile(path = klibPath),
            component = KLIB_DEFAULT_COMPONENT_NAME,
            target = target,
        ).manifestProperties
    }

    private fun loadManifestOfNativeKlib(klibPath: String, target: KonanTarget?): Properties {
        val result = KlibLoader {
            libraryPaths(klibPath)
            target?.let { manifestTransformer(KlibNativeManifestTransformer(target)) }
        }.load()
        assertFalse(result.hasProblems)
        assertEquals(1, result.librariesStdlibFirst.size)
        return result.librariesStdlibFirst[0].manifestProperties
    }

    companion object {
        private fun Properties.assertContainsAllProperties(properties: Map<String, String>) {
            properties.forEach { (key, expectedValue) ->
                val actualValue = getProperty(key)
                assertNotNull(actualValue, "Missing property: $key")
                assertEquals(expectedValue, actualValue, "Unexpected value for property $key")
            }
        }

        private const val BASE_SUBSTITUTED_PROPERTY = "compilerOpts"

        private val ALL_SUBSTITUTED_PROPERTY_NAMES = listOf(
            BASE_SUBSTITUTED_PROPERTY,
            "$BASE_SUBSTITUTED_PROPERTY.linux_arm64",
            "$BASE_SUBSTITUTED_PROPERTY.arm64",
            "$BASE_SUBSTITUTED_PROPERTY.linux",
        )

        private val CUSTOM_MANIFEST_PROPERTIES: Map<String, String> = ALL_SUBSTITUTED_PROPERTY_NAMES.associateBy { it }

        private val RESULTING_SUBSTITUTED_PROPERTY_VALUE = ALL_SUBSTITUTED_PROPERTY_NAMES.joinToString(" ")
    }
}
