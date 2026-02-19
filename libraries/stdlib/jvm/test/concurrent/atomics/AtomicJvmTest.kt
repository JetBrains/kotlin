/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(ExperimentalAtomicApi::class)

package test.concurrent.atomics

import kotlin.concurrent.atomics.*
import java.util.concurrent.atomic.*
import kotlin.test.*

class AtomicIntConversionTest {
    @Test
    fun asJavaAtomicTest() {
        val x = AtomicInt(0)
        assertEquals(0, x.asJavaAtomic().getAndSet(5))
        assertEquals(5, x.asJavaAtomic().get())
        assertEquals(5, x.exchange(7))
        assertEquals(7, x.asJavaAtomic().get())
    }

    @Test
    fun asKotlinAtomicTest() {
        val x = AtomicInteger(0)
        assertEquals(0, x.asKotlinAtomic().exchange(5))
        assertEquals(5, x.asKotlinAtomic().load())
        assertEquals(5, x.getAndSet(7))
        assertEquals(7, x.asKotlinAtomic().load())
    }
}

class AtomicLongConversionTest {
    @Test
    fun asJavaAtomicTest() {
        val x = kotlin.concurrent.atomics.AtomicLong(0L)
        assertEquals(0, x.asJavaAtomic().getAndSet(5))
        assertEquals(5, x.asJavaAtomic().get())
        assertEquals(5, x.exchange(7))
        assertEquals(7, x.asJavaAtomic().get())
    }

    @Test
    fun asKotlinAtomicTest() {
        val x = java.util.concurrent.atomic.AtomicLong(0L)
        assertEquals(0, x.asKotlinAtomic().exchange(5))
        assertEquals(5, x.asKotlinAtomic().load())
        assertEquals(5, x.getAndSet(7))
        assertEquals(7, x.asKotlinAtomic().load())
    }
}

class AtomicBooleanConversionTest {
    @Test
    fun asJavaAtomicTest() {
        val x = kotlin.concurrent.atomics.AtomicBoolean(true)
        assertTrue(x.asJavaAtomic().getAndSet(false))
        assertFalse(x.asJavaAtomic().get())
        assertFalse(x.exchange(true))
        assertTrue(x.asJavaAtomic().get())
    }

    @Test
    fun asKotlinAtomicTest() {
        val x = java.util.concurrent.atomic.AtomicBoolean(true)
        assertTrue(x.asKotlinAtomic().exchange(false))
        assertFalse(x.asKotlinAtomic().load())
        assertFalse(x.getAndSet(true))
        assertTrue(x.asKotlinAtomic().load())
    }
}

class AtomicReferenceConversionTest {
    @Test
    fun asJavaAtomicTest() {
        val x = kotlin.concurrent.atomics.AtomicReference("aaa")
        assertEquals("aaa", x.asJavaAtomic().getAndSet("bbb"))
        assertEquals("bbb", x.asJavaAtomic().get())
        assertEquals("bbb", x.exchange("ccc"))
        assertEquals("ccc", x.asJavaAtomic().get())
    }

    @Test
    fun asKotlinAtomicTest() {
        val x = java.util.concurrent.atomic.AtomicReference("aaa")
        assertEquals("aaa", x.asKotlinAtomic().exchange("bbb"))
        assertEquals("bbb", x.asKotlinAtomic().load())
        assertEquals("bbb", x.getAndSet("ccc"))
        assertEquals("ccc", x.asKotlinAtomic().load())
    }
}