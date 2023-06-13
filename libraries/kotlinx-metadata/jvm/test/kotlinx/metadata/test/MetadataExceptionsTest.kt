/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.metadata.test

import kotlinx.metadata.KmClass
import kotlinx.metadata.jvm.KotlinClassMetadata
import kotlinx.metadata.jvm.Metadata
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmMetadataVersion
import org.jetbrains.kotlin.protobuf.InvalidProtocolBufferException
import org.junit.Test
import kotlin.test.*

class MetadataExceptionsTest {
    @Test
    fun testReadMalformedInput() {
        val malformedInput = "abcdefdkdgwaydgyuawdfg543awyudfuiawty" // random string guaranteed by dropping ball on a keyboard
        val malformedMetadata =
            Metadata(KotlinClassMetadata.CLASS_KIND, KotlinClassMetadata.COMPATIBLE_METADATA_VERSION, arrayOf(malformedInput))
        val e = assertFailsWith<IllegalArgumentException> {
            (KotlinClassMetadata.read(malformedMetadata) as KotlinClassMetadata.Class)
        }
        assertIs<InvalidProtocolBufferException>(e.cause)
    }

    @Test
    fun testWriteMalformedClass() {
        val kmClass = KmClass() // kotlin.UninitializedPropertyAccessException: lateinit property name has not been initialized
        val e = assertFailsWith<IllegalArgumentException> {
            KotlinClassMetadata.writeClass(kmClass)
        }
        assertIs<UninitializedPropertyAccessException>(e.cause)
    }

    private fun doTestVersion(version: IntArray, expectedText: String) {
        val md = Metadata(metadataVersion = version)
        val iae = assertFailsWith<IllegalArgumentException> { KotlinClassMetadata.read(md) }
        assertContains(iae.message.orEmpty(), expectedText)
    }

    @Test
    fun testReadObsoleteVersion() {
        doTestVersion(intArrayOf(0, 1, 0), "version 0.1.0, while minimum supported version is 1.1.0")
        doTestVersion(intArrayOf(1, 0, 0), "version 1.0.0, while minimum supported version is 1.1.0")
        doTestVersion(intArrayOf(1, 0, 255), "version 1.0.255, while minimum supported version is 1.1.0")
    }

    @Test
    fun testReadNewerVersion() {
        val versionPlus2 = JvmMetadataVersion.INSTANCE.next().next()
        doTestVersion(
            versionPlus2.toArray(),
            "version $versionPlus2, while maximum supported version is ${JvmMetadataVersion.INSTANCE_NEXT}"
        )
    }

    @Test
    fun testInvalidVersion() {
        doTestVersion(intArrayOf(), "instance does not have metadataVersion in it and therefore is malformed and cannot be read")
        doTestVersion(
            JvmMetadataVersion.INVALID_VERSION.toArray(),
            "instance does not have metadataVersion in it and therefore is malformed and cannot be read"
        )
    }
}
