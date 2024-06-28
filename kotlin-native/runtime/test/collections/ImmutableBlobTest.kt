/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.collections

import kotlin.test.*
import kotlinx.cinterop.*

class ImmutableBlobTest {
    @Test fun iterator() {
        val blob = immutableBlobOf(1, 2, 3)
        val actual = buildList {
            for (b in blob) {
                add(b)
            }
        }
        assertContentEquals(listOf<Byte>(1, 2, 3), actual)
    }

    @Test fun toByteArray() {
        val blob = immutableBlobOf(1, 2, 3)
        val actual = blob.toByteArray()
        assertContentEquals(byteArrayOf(1, 2, 3), actual)
    }

    @Test fun toByteArraySlice() {
        val blob = immutableBlobOf(0, 0)
        val arr = blob.toByteArray(0, 1)
        assertEquals(1, arr.size)
        assertEquals(0, arr[0])
        assertFailsWith<IndexOutOfBoundsException> {
            blob.toByteArray(-1, 1)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            blob.toByteArray(0, -1)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            blob.toByteArray(0, 10)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            blob.toByteArray(10, 11)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            blob.toByteArray(10, 1)
        }
    }

    @Test fun toUByteArraySlice() {
        val blob = immutableBlobOf(0, 0)
        val arr = blob.toUByteArray(0, 1)
        assertEquals(1, arr.size)
        assertEquals(0U, arr[0])
        assertFailsWith<IndexOutOfBoundsException> {
            blob.toUByteArray(-1, 1)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            blob.toUByteArray(0, -1)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            blob.toUByteArray(0, 10)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            blob.toUByteArray(10, 11)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            blob.toUByteArray(10, 1)
        }
    }

    @Test fun asCPointer() {
        val blob = immutableBlobOf(0, 0)
        assertEquals(0, blob.asCPointer(0).pointed.value)
        assertFailsWith<IndexOutOfBoundsException> {
            blob.asCPointer(10)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            blob.asCPointer(-1)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            blob.asCPointer(Int.MAX_VALUE)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            blob.asCPointer(Int.MIN_VALUE)
        }
    }

    @Test fun asUCPointer() {
        val blob = immutableBlobOf(0, 0)
        assertEquals(0U, blob.asUCPointer(0).pointed.value)
        assertFailsWith<IndexOutOfBoundsException> {
            blob.asUCPointer(10)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            blob.asUCPointer(-1)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            blob.asUCPointer(Int.MAX_VALUE)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            blob.asUCPointer(Int.MIN_VALUE)
        }
    }
}