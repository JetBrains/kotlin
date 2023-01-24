/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.metadata.test

import kotlinx.metadata.KmClass
import kotlinx.metadata.jvm.KotlinClassMetadata
import kotlinx.metadata.jvm.Metadata
import org.jetbrains.kotlin.protobuf.InvalidProtocolBufferException
import org.junit.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class MetadataExceptionsTest {
    @Test
    fun testReadMalformedInput() {
        val malformedInput = "abcdefdkdgwaydgyuawdfg543awyudfuiawty" // random string guaranteed by dropping ball on a keyboard
        val malformedMetadata =
            Metadata(KotlinClassMetadata.CLASS_KIND, KotlinClassMetadata.COMPATIBLE_METADATA_VERSION, arrayOf(malformedInput))
        val e = assertFailsWith<IllegalArgumentException> {
            (KotlinClassMetadata.read(malformedMetadata) as KotlinClassMetadata.Class).toKmClass()
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
}