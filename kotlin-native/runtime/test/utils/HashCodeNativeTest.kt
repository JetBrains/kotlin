/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.utils

import kotlin.test.*

// Native-specific part of stdlib/test/utils/HashCodeTest.kt
class HashCodeNativeTest {
    @Test
    fun hashCodeOfInt() {
        assertEquals(239, 239.hashCode())
    }

    @Test
    fun hashCodeOfLong() {
        assertEquals(0, (-1L).hashCode())
    }

    @Test
    fun hashCodeOfChar() {
        assertEquals(97, 'a'.hashCode())
    }

    @Test
    fun hashCodeOfFloat() {
        assertEquals(1065353216, 1.0f.hashCode())
    }

    @Test
    fun hashCodeOfDouble() {
        assertEquals(1072693248, 1.0.hashCode())
    }

    @Test
    fun hashCodeOfBool() {
        assertEquals(1231, true.hashCode())
        assertEquals(1237, false.hashCode())
    }

    @Test
    fun hashCodeOfAny() {
        assertNotEquals(Any().hashCode(), Any().hashCode())
    }

    @Test
    fun hashCodeOfString() {
        val str = "Hello"
        val charArray = charArrayOf('H', 'e', 'l', 'l', 'o')
        assertEquals(str.hashCode(), charArray.concatToString(0, 5).hashCode())
    }
}