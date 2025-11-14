/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.metadata.klib

import kotlin.metadata.internal.common.KmModuleFragment
import org.jetbrains.kotlin.metadata.deserialization.MetadataVersion
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class KlibReadStrictLenientTest {
    @Test
    fun testReadingTooNewVersion() {
        val tooNewVersion = KlibMetadataVersion(MetadataVersion.INSTANCE.next().next().toArray())
        assertFailsWith<IllegalStateException> { readWithVersion(tooNewVersion, lenient = false) }
        val lenient = readWithVersion(tooNewVersion, lenient = true)
        assertTrue(!lenient.isAllowedToWrite)
    }

    @Test
    fun testWritingInLenientMode() {
        val current = KlibMetadataVersion(MetadataVersion.INSTANCE.toArray())
        val lenient = readWithVersion(current, lenient = true)
        assertFailsWith<IllegalStateException> { lenient.write() }
    }

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
