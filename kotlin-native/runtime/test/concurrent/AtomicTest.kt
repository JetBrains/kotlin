/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.concurrent

import kotlin.concurrent.*
import kotlin.native.internal.NativePtr
import kotlin.test.*

data class Data(val value: Int)

class AtomicIntTest {
    @Test fun ctor() {
        val x = AtomicInt(0)
        assertEquals(x.value, 0)
    }

    @Test fun setter() {
        val x = AtomicInt(0)
        x.value = 1
        assertEquals(x.value, 1)
    }

    @Test fun addAndGet() {
        val x = AtomicInt(1)
        val result = x.addAndGet(2)
        assertEquals(result, 1 + 2)
        assertEquals(x.value, result)
    }

    @Test fun compareAndExchange() {
        val x = AtomicInt(0)
        val successValue = x.compareAndExchange(0, 1)
        assertEquals(successValue, 0)
        assertEquals(x.value, 1)
        val failValue = x.compareAndExchange(0, 2)
        assertEquals(failValue, 1)
        assertEquals(x.value, 1)
    }

    @Test fun compareAndSet() {
        val x = AtomicInt(0)
        val successValue = x.compareAndSet(0, 1)
        assertTrue(successValue)
        assertEquals(x.value, 1)
        val failValue = x.compareAndSet(0, 2)
        assertFalse(failValue)
        assertEquals(x.value, 1)
    }

    @Test fun smoke() {
        val atomic = AtomicInt(3)
        assertEquals(3, atomic.value)
        atomic.value = 5
        assertEquals(5, atomic.value)
        assertEquals(5, atomic.getAndSet(6))
        assertEquals(6, atomic.value)
        assertTrue(atomic.compareAndSet(6, 8))
        assertFalse(atomic.compareAndSet(9, 1))
        assertEquals(8, atomic.value)
        assertEquals(8, atomic.getAndAdd(5))
        assertEquals(13, atomic.value)
        assertEquals(18, atomic.addAndGet(5))
        assertEquals(18, atomic.getAndIncrement())
        assertEquals(19, atomic.value)
        assertEquals(20, atomic.incrementAndGet())
        assertEquals(20, atomic.value)
        assertEquals(20, atomic.getAndDecrement())
        assertEquals(19, atomic.value)
        assertEquals(18, atomic.decrementAndGet())
        assertEquals(18, atomic.value)
        assertEquals(18, atomic.compareAndExchange(18, 56))
        assertEquals(56, atomic.compareAndExchange(18, 56))
        assertEquals(56, atomic.compareAndExchange(18, 56))
    }
}

class AtomicLongTest {
    @Test fun ctor() {
        val x = AtomicLong(0)
        assertEquals(x.value, 0)
    }

    @Test fun setter() {
        val x = AtomicLong(0)
        x.value = 1
        assertEquals(x.value, 1)
    }

    @Test fun addAndGet() {
        val x = AtomicLong(1)
        val result = x.addAndGet(2L)
        assertEquals(result, 1 + 2)
        assertEquals(x.value, result)
    }

    @Test fun compareAndExchange() {
        val x = AtomicLong(0)
        val successValue = x.compareAndExchange(0, 1)
        assertEquals(successValue, 0)
        assertEquals(x.value, 1)
        val failValue = x.compareAndExchange(0, 2)
        assertEquals(failValue, 1)
        assertEquals(x.value, 1)
    }

    @Test fun compareAndSet() {
        val x = AtomicLong(0)
        val successValue = x.compareAndSet(0, 1)
        assertTrue(successValue)
        assertEquals(x.value, 1)
        val failValue = x.compareAndSet(0, 2)
        assertFalse(failValue)
        assertEquals(x.value, 1)
    }

    @Test fun smoke() {
        val atomic = AtomicLong(1424920024888900000)
        assertEquals(1424920024888900000, atomic.value)
        atomic.value = 2424920024888900000
        assertEquals(2424920024888900000, atomic.value)
        assertEquals(2424920024888900000, atomic.getAndSet(3424920024888900000))
        assertEquals(3424920024888900000, atomic.value)
        assertTrue(atomic.compareAndSet(3424920024888900000, 4424920024888900000))
        assertFalse(atomic.compareAndSet(9, 1))
        assertEquals(4424920024888900000, atomic.value)
        assertEquals(4424920024888900000, atomic.getAndAdd(100000))
        assertEquals(4424920024889000000, atomic.value)
        assertEquals(4424920024890000000, atomic.addAndGet(1000000L))
        assertEquals(4424920024890000000, atomic.getAndIncrement())
        assertEquals(4424920024890000001, atomic.value)
        assertEquals(4424920024890000002, atomic.incrementAndGet())
        assertEquals(4424920024890000002, atomic.value)
        assertEquals(4424920024890000002, atomic.getAndDecrement())
        assertEquals(4424920024890000001, atomic.value)
        assertEquals(4424920024890000000, atomic.decrementAndGet())
        assertEquals(4424920024890000000, atomic.value)
        assertEquals(4424920024890000000, atomic.compareAndExchange(4424920024890000000, 5424920024890000000))
        assertEquals(5424920024890000000, atomic.compareAndExchange(18, 56))
        assertEquals(5424920024890000000, atomic.compareAndExchange(18, 56))
    }
}

class AtomicReferenceTest {
    @Test fun ctor() {
        val x = AtomicReference(Data(1))
        assertEquals(x.value, Data(1))
    }

    @Test fun setter() {
        val x = AtomicReference<Data>(Data(1))
        x.value = Data(2)
        assertEquals(x.value, Data(2))
    }

    @Test fun compareAndExchange() {
        val initial = Data(1)
        val new = Data(2)
        val x = AtomicReference<Data>(initial)
        val successValue = x.compareAndExchange(initial, new)
        assertEquals(successValue, initial)
        assertEquals(x.value, new)
        val failValue = x.compareAndExchange(initial, Data(3))
        assertEquals(failValue, new)
        assertEquals(x.value, new)
    }

    @Test fun compareAndSet() {
        val initial = Data(1)
        val new = Data(2)
        val x = AtomicReference<Data>(initial)
        val successValue = x.compareAndSet(initial, new)
        assertTrue(successValue)
        assertEquals(x.value, new)
        val failValue = x.compareAndSet(initial, Data(2))
        assertFalse(failValue)
        assertEquals(x.value, new)
    }

    @Test
    fun smoke() {
        val atomic = AtomicReference<List<Data>>(listOf(Data(1), Data(2), Data(3)))
        assertEquals(listOf(Data(1), Data(2), Data(3)), atomic.value)
        atomic.value = listOf(Data(1), Data(2), Data(1))
        assertEquals(listOf(Data(1), Data(2), Data(1)), atomic.value)
        assertEquals(listOf(Data(1), Data(2), Data(1)), atomic.getAndSet(listOf(Data(1), Data(1), Data(1))))
        assertEquals(listOf(Data(1), Data(1), Data(1)), atomic.value)
        var cur = atomic.value
        assertTrue(atomic.compareAndSet(cur, listOf(Data(2), Data(2), Data(2))))
        assertFalse(atomic.compareAndSet(listOf(Data(1), Data(1), Data(1)), listOf(Data(2), Data(2), Data(2))))
        assertEquals(listOf(Data(2), Data(2), Data(2)), atomic.value)
        cur = atomic.value
        assertEquals(listOf(Data(2), Data(2), Data(2)), atomic.compareAndExchange(cur, listOf(Data(3), Data(3), Data(3))))
        assertEquals(listOf(Data(3), Data(3), Data(3)), atomic.compareAndExchange(cur, listOf(Data(4), Data(4), Data(4))))
        assertEquals(listOf(Data(3), Data(3), Data(3)), atomic.compareAndExchange(cur, listOf(Data(3), Data(3), Data(3))))
    }
}

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