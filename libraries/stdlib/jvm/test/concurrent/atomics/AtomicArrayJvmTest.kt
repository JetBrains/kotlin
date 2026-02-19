/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(ExperimentalAtomicApi::class)

package test.concurrent.atomics

import kotlin.concurrent.atomics.*
import java.util.concurrent.atomic.*
import kotlin.test.*

class AtomicIntArrayConversionTest {
    @Test
    fun asJavaAtomicArrayTest() {
        val x = AtomicIntArray(5)
        assertEquals(0, x.asJavaAtomicArray().getAndSet(2, 5))
        assertEquals(5, x.asJavaAtomicArray().get(2))
        assertEquals(5, x.exchangeAt(2, 7))
        assertEquals(7, x.asJavaAtomicArray().get(2))
    }

    @Test
    fun asKotlinAtomicTest() {
        val x = AtomicIntegerArray(IntArray(5))
        assertEquals(0, x.asKotlinAtomicArray().exchangeAt(2, 5))
        assertEquals(5, x.asKotlinAtomicArray().loadAt(2))
        assertEquals(5, x.getAndSet(2, 7))
        assertEquals(7, x.asKotlinAtomicArray().loadAt(2))
    }
}

class AtomicLongArrayConversionTest {
    @Test
    fun asJavaAtomicArrayTest() {
        val x = kotlin.concurrent.atomics.AtomicLongArray(5)
        assertEquals(0, x.asJavaAtomicArray().getAndSet(2, 5))
        assertEquals(5, x.asJavaAtomicArray().get(2))
        assertEquals(5, x.exchangeAt(2, 7))
        assertEquals(7, x.asJavaAtomicArray().get(2))
    }

    @Test
    fun asKotlinAtomicArrayTest() {
        val x = java.util.concurrent.atomic.AtomicLongArray(LongArray(5))
        assertEquals(0, x.asKotlinAtomicArray().exchangeAt(2, 5))
        assertEquals(5, x.asKotlinAtomicArray().loadAt(2))
        assertEquals(5, x.getAndSet(2, 7))
        assertEquals(7, x.asKotlinAtomicArray().loadAt(2))
    }
}

class AtomicReferenceArrayConversionTest {
    @Test
    fun asJavaAtomicArrayTest() {
        val x = kotlin.concurrent.atomics.AtomicArray(arrayOf("a", "b", "c"))
        assertEquals("a", x.asJavaAtomicArray().getAndSet(0,"foo"))
        assertEquals("foo", x.asJavaAtomicArray().get(0))
        assertEquals("foo", x.exchangeAt(0, "bar"))
        assertEquals("bar", x.asJavaAtomicArray().get(0))
    }

    @Test
    fun asKotlinAtomicArrayTest() {
        val x = java.util.concurrent.atomic.AtomicReferenceArray(arrayOf("a", "b", "c"))
        assertEquals("a", x.asKotlinAtomicArray().exchangeAt(0, "foo"))
        assertEquals("foo", x.asKotlinAtomicArray().loadAt(0))
        assertEquals("foo", x.getAndSet(0, "bar"))
        assertEquals("bar", x.asKotlinAtomicArray().loadAt(0))
    }
}