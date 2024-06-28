/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.collections

import kotlin.test.*

// Native-specific part of stdlib/test/collections/UnsignedArraysTest.kt
class UnsignedArraysNativeTest {
    @Test fun ubyteArray() {
        assertFailsWith<IllegalArgumentException> { UByteArray(-1) }
    }

    @Test fun ubyteArrayInit() {
        assertFailsWith<IllegalArgumentException> { UByteArray(-1) { it.toUByte() } }
    }

    @Test fun ushortArray() {
        assertFailsWith<IllegalArgumentException> { UShortArray(-1) }
    }

    @Test fun ushortArrayInit() {
        assertFailsWith<IllegalArgumentException> { UShortArray(-1) { it.toUShort() } }
    }

    @Test fun uintArray() {
        assertFailsWith<IllegalArgumentException> { UIntArray(-1) }
    }

    @Test fun uintArrayInit() {
        assertFailsWith<IllegalArgumentException> { UIntArray(-1) { it.toUInt() } }
    }

    @Test fun ulongArray() {
        assertFailsWith<IllegalArgumentException> { ULongArray(-1) }
    }

    @Test fun ulongArrayInit() {
        assertFailsWith<IllegalArgumentException> { ULongArray(-1) { it.toULong() } }
    }

    @Test fun ubyteArrayGetOutOfBounds() {
        val arr = UByteArray(5)
        assertFailsWith<IndexOutOfBoundsException> {
            arr[arr.size]
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr[-1]
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr[Int.MAX_VALUE]
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr[Int.MIN_VALUE]
        }
    }

    @Test fun ushortArrayGetOutOfBounds() {
        val arr = UShortArray(5)
        assertFailsWith<IndexOutOfBoundsException> {
            arr[arr.size]
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr[-1]
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr[Int.MAX_VALUE]
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr[Int.MIN_VALUE]
        }
    }

    @Test fun uintArrayGetOutOfBounds() {
        val arr = UIntArray(5)
        assertFailsWith<IndexOutOfBoundsException> {
            arr[arr.size]
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr[-1]
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr[Int.MAX_VALUE]
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr[Int.MIN_VALUE]
        }
    }

    @Test fun ubyteArraySetOutOfBounds() {
        val arr = UByteArray(5)
        assertFailsWith<IndexOutOfBoundsException> {
            arr[arr.size] = 1U
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr[-1] = 1U
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr[Int.MAX_VALUE] = 1U
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr[Int.MIN_VALUE] = 1U
        }
    }

    @Test fun ushortArraySetOutOfBounds() {
        val arr = UShortArray(5)
        assertFailsWith<IndexOutOfBoundsException> {
            arr[arr.size] = 1U
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr[-1] = 1U
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr[Int.MAX_VALUE] = 1U
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr[Int.MIN_VALUE] = 1U
        }
    }

    @Test fun uintArraySetOutOfBounds() {
        val arr = UIntArray(5)
        assertFailsWith<IndexOutOfBoundsException> {
            arr[arr.size] = 1U
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr[-1] = 1U
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr[Int.MAX_VALUE] = 1U
        }
        assertFailsWith<IndexOutOfBoundsException> {
            arr[Int.MIN_VALUE] = 1U
        }
    }
}