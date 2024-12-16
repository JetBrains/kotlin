/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.concurrent

import kotlin.concurrent.*
import kotlin.native.internal.NativePtr
import kotlin.test.*

class AtomicNativePtrTest {
    @Test fun smoke() {
        val atomic = AtomicNativePtr(NativePtr.NULL)
        assertEquals(NativePtr.NULL, atomic.value)
        atomic.value = NativePtr.NULL.plus(10L)
        assertEquals(10L, atomic.value.toLong())
        assertTrue(atomic.compareAndSet(NativePtr.NULL.plus(10L), NativePtr.NULL.plus(20L)))
        assertEquals(20L, atomic.value.toLong())
        assertFalse(atomic.compareAndSet(NativePtr.NULL.plus(10L), NativePtr.NULL.plus(20L)))
        assertEquals(20L, atomic.value.toLong())
        assertEquals(NativePtr.NULL.plus(20L), atomic.compareAndExchange(NativePtr.NULL.plus(20L), NativePtr.NULL.plus(30L)))
        assertEquals(NativePtr.NULL.plus(30L), atomic.compareAndExchange(NativePtr.NULL.plus(20L), NativePtr.NULL.plus(40L)))
        assertEquals(NativePtr.NULL.plus(30L), atomic.compareAndExchange(NativePtr.NULL.plus(20L), NativePtr.NULL.plus(50L)))
        assertEquals(30L, atomic.getAndSet(NativePtr.NULL.plus(55L)).toLong())
        assertEquals(55L, atomic.value.toLong())
    }
}