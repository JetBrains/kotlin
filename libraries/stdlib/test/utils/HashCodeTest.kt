/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HashCodeTest {
    @Test
    fun hashCodeOfNull() {
        assertEquals(0, null.hashCode())

        val foo: Any? = null
        assertEquals(0, foo.hashCode())
    }

    @Test
    fun hashCodeOfNotNull() {
        val value = "test"
        val nullableValue: String? = value

        assertEquals(value.hashCode(), nullableValue.hashCode())
    }

    // KT-86954: all NaN bit patterns must produce the same hash code, matching equals() semantics.
    @Test
    fun hashCodeOfNaN() {
        val canonicalDoubleNaN = Double.NaN
        val otherDoubleNaN = Double.fromBits(canonicalDoubleNaN.toBits() or 1L)
        assertTrue(otherDoubleNaN.isNaN())
        assertTrue(canonicalDoubleNaN.equals(otherDoubleNaN))
        assertEquals(canonicalDoubleNaN.hashCode(), otherDoubleNaN.hashCode())

        val canonicalFloatNaN = Float.NaN
        val otherFloatNaN = Float.fromBits(0xFFFC0000.toInt())
        assertTrue(otherFloatNaN.isNaN())
        assertTrue(canonicalFloatNaN.equals(otherFloatNaN))
        assertEquals(canonicalFloatNaN.hashCode(), otherFloatNaN.hashCode())
    }
}
