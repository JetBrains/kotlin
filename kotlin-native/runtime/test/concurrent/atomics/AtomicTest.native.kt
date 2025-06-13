/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalAtomicApi::class)

package test.concurrent.atomics

import kotlin.concurrent.atomics.*
import kotlin.native.internal.NativePtr
import kotlin.test.*

class AtomicNativePtrTest {
    @Test fun smoke() {
        val atomic = AtomicNativePtr(NativePtr.NULL)
        assertEquals(NativePtr.NULL, atomic.load())
        atomic.store(NativePtr.NULL.plus(10L))
        assertEquals(10L, atomic.load().toLong())
        assertTrue(atomic.compareAndSet(NativePtr.NULL.plus(10L), NativePtr.NULL.plus(20L)))
        assertEquals(20L, atomic.load().toLong())
        assertFalse(atomic.compareAndSet(NativePtr.NULL.plus(10L), NativePtr.NULL.plus(20L)))
        assertEquals(20L, atomic.load().toLong())
        assertEquals(NativePtr.NULL.plus(20L), atomic.compareAndExchange(NativePtr.NULL.plus(20L), NativePtr.NULL.plus(30L)))
        assertEquals(NativePtr.NULL.plus(30L), atomic.compareAndExchange(NativePtr.NULL.plus(20L), NativePtr.NULL.plus(40L)))
        assertEquals(NativePtr.NULL.plus(30L), atomic.compareAndExchange(NativePtr.NULL.plus(20L), NativePtr.NULL.plus(50L)))
        assertEquals(30L, atomic.exchange(NativePtr.NULL.plus(55L)).toLong())
        assertEquals(55L, atomic.load().toLong())
    }

    @Test fun testUpdate() {
        val atomic = AtomicNativePtr(NativePtr.NULL)
        val ptrX4000: NativePtr = NativePtr.NULL.plus(0x4000L)
        val ptrX8000: NativePtr = NativePtr.NULL.plus(0x8000L)

        assertEquals(NativePtr.NULL, atomic.fetchAndUpdate { _ -> ptrX4000 })
        assertEquals(ptrX4000, atomic.load())
        assertEquals(ptrX8000, atomic.updateAndFetch { _ -> ptrX8000 })

        atomic.update { ptr -> ptr.plus(100L) }
        assertEquals(ptrX8000.plus(100L), atomic.load())
    }
}
