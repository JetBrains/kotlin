/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.metadata.klib

import org.jetbrains.kotlin.metadata.deserialization.MetadataVersion
import kotlin.metadata.internal.common.KmModuleFragment
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class KlibReadStrictLenientTest {
    @Test
    fun testReadingSupportedVersions() {
        val previousLenient = readWithVersion(previousVersion, lenient = true)
        val previousStrict = readWithVersion(previousVersion, lenient = false)
        assertEquals(previousVersion, previousLenient.metadataVersion)
        assertEquals(previousVersion, previousStrict.metadataVersion)

        val currentLenient = readWithVersion(currentVersion, lenient = true)
        val currentStrict = readWithVersion(currentVersion, lenient = false)
        assertEquals(currentVersion, currentLenient.metadataVersion)
        assertEquals(currentVersion, currentStrict.metadataVersion)

        val nextLenient = readWithVersion(nextVersion, lenient = true)
        val nextStrict = readWithVersion(nextVersion, lenient = false)
        assertEquals(nextVersion, nextLenient.metadataVersion)
        assertEquals(nextVersion, nextStrict.metadataVersion)
    }

    @Test
    fun testLenientReadingOfNotSupportedVersion() {
        val lenient = readWithVersion(notSupportedVersion, lenient = true)
        assertEquals(notSupportedVersion, lenient.metadataVersion)
    }

    @Test
    fun testStrictReadingOfNotSupportedVersion() {
        assertFailsWith<IllegalStateException> { readWithVersion(notSupportedVersion, lenient = false) }
    }

    @Test
    fun testWritingInLenientMode() {
        val lenient = readWithVersion(currentVersion, lenient = true)
        assertFailsWith<IllegalStateException> { lenient.write() }
    }

    private val previousVersion = KlibMetadataVersion(intArrayOf(2, 0, 0))
    private val currentVersion = KlibMetadataVersion(MetadataVersion.INSTANCE.toArray())
    private val nextVersion = KlibMetadataVersion(MetadataVersion.INSTANCE.next().toArray())
    private val notSupportedVersion = KlibMetadataVersion(MetadataVersion.INSTANCE.next().next().toArray())

    private fun readWithVersion(version: KlibMetadataVersion, lenient: Boolean): KlibModuleMetadata {
        val metadata = KlibModuleMetadata(
            name = "klib",
            fragments = listOf(KmModuleFragment().apply { fqName = "klib" }),
            annotations = emptyList(),
            metadataVersion = version,
        ).write()
        val provider = object : KlibModuleMetadata.MetadataLibraryProvider {
            override val moduleHeaderData: ByteArray get() = metadata.header
            override val metadataVersion: KlibMetadataVersion = metadata.metadataVersion
            override fun packageMetadataParts(fqName: String): Set<String> = metadata.fragmentNames.toSet()
            override fun packageMetadata(fqName: String, partName: String): ByteArray = metadata.fragments.single().single()
        }
        return if (lenient) KlibModuleMetadata.readLenient(provider) else KlibModuleMetadata.readStrict(provider)
    }
}
