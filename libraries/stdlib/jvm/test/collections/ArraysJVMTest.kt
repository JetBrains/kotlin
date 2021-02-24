/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.collections

import java.util.Collections
import kotlin.test.*


class ArraysJVMTest {

    @Suppress("HasPlatformType", "UNCHECKED_CAST")
    fun <T> platformNull() = Collections.singletonList(null as T).first()

    @Test
    fun contentEquals() {
        assertTrue(platformNull<IntArray>() contentEquals null)
        assertTrue(null contentEquals platformNull<LongArray>())

        assertFalse(platformNull<Array<String>>() contentEquals emptyArray<String>())
        assertFalse(arrayOf("a", "b") contentEquals platformNull<Array<String>>())

        assertFalse(platformNull<UShortArray>() contentEquals ushortArrayOf())
        assertFalse(ubyteArrayOf() contentEquals platformNull<UByteArray>())
    }

    @Test
    fun contentHashCode() {
        assertEquals(0, platformNull<Array<Int>>().contentHashCode())
        assertEquals(0, platformNull<CharArray>().contentHashCode())
        assertEquals(0, platformNull<ShortArray>().contentHashCode())
        assertEquals(0, platformNull<BooleanArray>().contentHashCode())
        assertEquals(0, platformNull<UByteArray>().contentHashCode())
        assertEquals(0, platformNull<UIntArray>().contentHashCode())
    }

    @Test
    fun contentToString() {
        assertEquals("null", platformNull<Array<String>>().contentToString())
        assertEquals("null", platformNull<CharArray>().contentToString())
        assertEquals("null", platformNull<DoubleArray>().contentToString())
        assertEquals("null", platformNull<FloatArray>().contentToString())
        assertEquals("null", platformNull<ULongArray>().contentToString())
    }

    @Test
    fun contentDeepEquals() {
        assertFalse(platformNull<Array<String>>() contentDeepEquals emptyArray<String>())
        assertFalse(arrayOf("a", "b") contentDeepEquals platformNull<Array<String>>())
    }

    @Test
    fun contentDeepHashCode() {
        assertEquals(0, platformNull<Array<Int>>().contentDeepHashCode())
    }

    @Test
    fun contentDeepToString() {
        assertEquals("null", platformNull<Array<String>>().contentDeepToString())
    }
}