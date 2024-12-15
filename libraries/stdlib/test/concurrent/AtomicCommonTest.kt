/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(ExperimentalStdlibApi::class)

package test.concurrent

import kotlin.concurrent.atomics.*
import kotlin.test.*

data class Data(val value: Int)

class AtomicIntTest {
    @Test
    fun ctor() {
        val x = AtomicInt(0)
        assertEquals(0, x.load())
    }

    @Test
    fun setter() {
        val x = AtomicInt(0)
        x.store(1)
        assertEquals(1, x.load())
    }

    @Test
    fun addAndGet() {
        val x = AtomicInt(1)
        val result = x.addAndFetch(2)
        assertEquals(1 + 2, result)
        assertEquals(result, x.load())
    }

    @Test
    fun compareAndExchange() {
        val x = AtomicInt(0)
        val successValue = x.compareAndExchange(0, 1)
        assertEquals(0, successValue)
        assertEquals(1, x.load())
        val failValue = x.compareAndExchange(0, 2)
        assertEquals(1, failValue)
        assertEquals(1, x.load())
    }

    @Test
    fun compareAndSet() {
        val x = AtomicInt(0)
        val successValue = x.compareAndSet(0, 1)
        assertTrue(successValue)
        assertEquals(1, x.load())
        val failValue = x.compareAndSet(0, 2)
        assertFalse(failValue)
        assertEquals(1, x.load())
    }

    @Test
    fun smoke() {
        val atomic = AtomicInt(3)
        assertEquals(3, atomic.load())
        atomic.store(5)
        assertEquals(5, atomic.load())
        assertEquals(5, atomic.exchange(6))
        assertEquals(6, atomic.load())
        assertTrue(atomic.compareAndSet(6, 8))
        assertFalse(atomic.compareAndSet(9, 1))
        assertEquals(8, atomic.load())
        assertEquals(8, atomic.fetchAndAdd(5))
        assertEquals(13, atomic.load())
        assertEquals(18, atomic.addAndFetch(5))
        assertEquals(18, atomic.fetchAndIncrement())
        assertEquals(19, atomic.load())
        assertEquals(20, atomic.incrementAndFetch())
        assertEquals(20, atomic.load())
        assertEquals(20, atomic.fetchAndDecrement())
        assertEquals(19, atomic.load())
        assertEquals(18, atomic.decrementAndFetch())
        assertEquals(18, atomic.load())
        assertEquals(18, atomic.compareAndExchange(18, 56))
        assertEquals(56, atomic.compareAndExchange(18, 56))
        assertEquals(56, atomic.compareAndExchange(18, 56))
    }
}

class AtomicLongTest {
    @Test
    fun ctor() {
        val x = AtomicLong(0)
        assertEquals(0, x.load())
    }

    @Test
    fun setter() {
        val x = AtomicLong(0)
        x.store(1)
        assertEquals(1, x.load())
    }

    @Test
    fun addAndGet() {
        val x = AtomicLong(1)
        val result = x.addAndFetch(2L)
        assertEquals(1 + 2, result)
        assertEquals(result, x.load())
    }

    @Test
    fun compareAndExchange() {
        val x = AtomicLong(0)
        val successValue = x.compareAndExchange(0, 1)
        assertEquals(0, successValue)
        assertEquals(1, x.load())
        val failValue = x.compareAndExchange(0, 2)
        assertEquals(1, failValue)
        assertEquals(1, x.load())
    }

    @Test
    fun compareAndSet() {
        val x = AtomicLong(0)
        val successValue = x.compareAndSet(0, 1)
        assertTrue(successValue)
        assertEquals(1, x.load())
        val failValue = x.compareAndSet(0, 2)
        assertFalse(failValue)
        assertEquals(1, x.load())
    }

    @Test
    fun smoke() {
        val atomic = AtomicLong(1424920024888900000)
        assertEquals(1424920024888900000, atomic.load())
        atomic.store(2424920024888900000)
        assertEquals(2424920024888900000, atomic.load())
        assertEquals(2424920024888900000, atomic.exchange(3424920024888900000))
        assertEquals(3424920024888900000, atomic.load())
        assertTrue(atomic.compareAndSet(3424920024888900000, 4424920024888900000))
        assertFalse(atomic.compareAndSet(9, 1))
        assertEquals(4424920024888900000, atomic.load())
        assertEquals(4424920024888900000, atomic.fetchAndAdd(100000))
        assertEquals(4424920024889000000, atomic.load())
        assertEquals(4424920024890000000, atomic.addAndFetch(1000000L))
        assertEquals(4424920024890000000, atomic.fetchAndIncrement())
        assertEquals(4424920024890000001, atomic.load())
        assertEquals(4424920024890000002, atomic.incrementAndFetch())
        assertEquals(4424920024890000002, atomic.load())
        assertEquals(4424920024890000002, atomic.fetchAndDecrement())
        assertEquals(4424920024890000001, atomic.load())
        assertEquals(4424920024890000000, atomic.decrementAndFetch())
        assertEquals(4424920024890000000, atomic.load())
        assertEquals(4424920024890000000, atomic.compareAndExchange(4424920024890000000, 5424920024890000000))
        assertEquals(5424920024890000000, atomic.compareAndExchange(18, 56))
        assertEquals(5424920024890000000, atomic.compareAndExchange(18, 56))
    }
}

class AtomicBooleanTest {
    @Test
    fun ctor() {
        val x = AtomicBoolean(true)
        assertTrue(x.load())
    }

    @Test
    fun setter() {
        val x = AtomicBoolean(true)
        x.store(false)
        assertFalse(x.load())
    }

    @Test
    fun compareAndExchange() {
        val x = AtomicBoolean(true)
        val successValue = x.compareAndExchange(true, false)
        assertTrue(successValue)
        assertFalse(x.load())
        val failValue = x.compareAndExchange(true, false)
        assertFalse(failValue)
        assertFalse(x.load())
    }

    @Test
    fun compareAndSet() {
        val x = AtomicBoolean(true)
        val successValue = x.compareAndSet(true, false)
        assertTrue(successValue)
        assertFalse(x.load())
        val failValue = x.compareAndSet(true, false)
        assertFalse(failValue)
        assertFalse(x.load())
    }

    @Test
    fun exchange() {
        val atomic = AtomicBoolean(true)
        assertTrue(atomic.load())
        atomic.store(false)
        assertFalse(atomic.load())
        assertFalse(atomic.exchange(true))
        assertTrue(atomic.load())
    }
}

class AtomicReferenceTest {
    @Test
    fun ctor() {
        val x = AtomicReference(Data(1))
        assertEquals(x.load(), Data(1))
    }

    @Test
    fun setter() {
        val x = AtomicReference<Data>(Data(1))
        x.store(Data(2))
        assertEquals(x.load(), Data(2))
    }

    @Test
    fun compareAndExchange() {
        val initial = Data(1)
        val new = Data(2)
        val x = AtomicReference<Data>(initial)
        val successValue = x.compareAndExchange(initial, new)
        assertEquals(successValue, initial)
        assertEquals(x.load(), new)
        val failValue = x.compareAndExchange(initial, Data(3))
        assertEquals(failValue, new)
        assertEquals(x.load(), new)
    }

    @Test
    fun compareAndSet() {
        val initial = Data(1)
        val new = Data(2)
        val x = AtomicReference<Data>(initial)
        val successValue = x.compareAndSet(initial, new)
        assertTrue(successValue)
        assertEquals(x.load(), new)
        val failValue = x.compareAndSet(initial, Data(2))
        assertFalse(failValue)
        assertEquals(x.load(), new)
    }

    @Test
    fun smoke() {
        val atomic = AtomicReference<List<Data>>(listOf(Data(1), Data(2), Data(3)))
        assertEquals(listOf(Data(1), Data(2), Data(3)), atomic.load())
        atomic.store(listOf(Data(1), Data(2), Data(1)))
        assertEquals(listOf(Data(1), Data(2), Data(1)), atomic.load())
        assertEquals(listOf(Data(1), Data(2), Data(1)), atomic.exchange(listOf(Data(1), Data(1), Data(1))))
        assertEquals(listOf(Data(1), Data(1), Data(1)), atomic.load())
        var cur = atomic.load()
        assertTrue(atomic.compareAndSet(cur, listOf(Data(2), Data(2), Data(2))))
        assertFalse(atomic.compareAndSet(listOf(Data(1), Data(1), Data(1)), listOf(Data(2), Data(2), Data(2))))
        assertEquals(listOf(Data(2), Data(2), Data(2)), atomic.load())
        cur = atomic.load()
        assertEquals(listOf(Data(2), Data(2), Data(2)), atomic.compareAndExchange(cur, listOf(Data(3), Data(3), Data(3))))
        assertEquals(listOf(Data(3), Data(3), Data(3)), atomic.compareAndExchange(cur, listOf(Data(4), Data(4), Data(4))))
        assertEquals(listOf(Data(3), Data(3), Data(3)), atomic.compareAndExchange(cur, listOf(Data(3), Data(3), Data(3))))
    }
}