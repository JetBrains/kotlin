/*
 * Copyright 2016-2024 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests

import kotlinx.validation.api.klib.KlibSignatureVersion
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

class KlibSignatureVersionTest {
    @Test
    fun signatureConstruction() {
        assertFailsWith<IllegalArgumentException> { KlibSignatureVersion.of(-1) }
        assertFailsWith<IllegalArgumentException> { KlibSignatureVersion.of(0) }

        val correctVersion = KlibSignatureVersion.of(42)
        assertEquals(42, correctVersion.version)
    }

    @Test
    fun signaturesEqual() {
        assertEquals(KlibSignatureVersion.of(1), KlibSignatureVersion.of(1))
        KlibSignatureVersion.of(2).also {
            assertEquals(it, it)
        }

        assertNotEquals(KlibSignatureVersion.of(2), KlibSignatureVersion.of(3))
    }

    @Test
    fun signatureHashCode() {
        assertEquals(KlibSignatureVersion.of(1).hashCode(), KlibSignatureVersion.of(1).hashCode())
        assertNotEquals(KlibSignatureVersion.of(1).hashCode(), KlibSignatureVersion.of(2).hashCode())
    }

    @Test
    fun toStringFormat() {
        assertEquals("KlibSignatureVersion(LATEST)", KlibSignatureVersion.LATEST.toString())
        assertEquals("KlibSignatureVersion(42)", KlibSignatureVersion.of(42).toString())
    }
}
