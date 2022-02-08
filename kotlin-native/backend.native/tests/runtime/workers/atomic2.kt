/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

import kotlin.native.concurrent.*

@Test
fun ctor_Int() {
    val x = AtomicInt(0)
    assertEquals(x.value, 0)
}

@Test
fun ctor_Long() {
    val x = AtomicLong(0)
    assertEquals(x.value, 0)
}

@Test
fun setter_Int() {
    val x = AtomicInt(0)
    x.value = 1
    assertEquals(x.value, 1)
}

@Test
fun setter_Long() {
    val x = AtomicLong(0)
    x.value = 1
    assertEquals(x.value, 1)
}

@Test
fun addAndGet_Int() {
    val x = AtomicInt(1)
    val result = x.addAndGet(2)
    assertEquals(result, 1 + 2)
    assertEquals(x.value, result)
}

@Test
fun addAndGet_Long() {
    val x = AtomicLong(1)
    val result = x.addAndGet(2)
    assertEquals(result, 1 + 2)
    assertEquals(x.value, result)
}

@Test
fun compareAndSwap_Int() {
    val x = AtomicInt(0)
    val successValue = x.compareAndSwap(0, 1)
    assertEquals(successValue, 0)
    assertEquals(x.value, 1)
    val failValue = x.compareAndSwap(0, 2)
    assertEquals(failValue, 1)
    assertEquals(x.value, 1)
}

@Test
fun compareAndSwap_Long() {
    val x = AtomicLong(0)
    val successValue = x.compareAndSwap(0, 1)
    assertEquals(successValue, 0)
    assertEquals(x.value, 1)
    val failValue = x.compareAndSwap(0, 2)
    assertEquals(failValue, 1)
    assertEquals(x.value, 1)
}

@Test
fun compareAndSet_Int() {
    val x = AtomicInt(0)
    val successValue = x.compareAndSet(0, 1)
    assertTrue(successValue)
    assertEquals(x.value, 1)
    val failValue = x.compareAndSet(0, 2)
    assertFalse(failValue)
    assertEquals(x.value, 1)
}

@Test
fun compareAndSet_Long() {
    val x = AtomicLong(0)
    val successValue = x.compareAndSet(0, 1)
    assertTrue(successValue)
    assertEquals(x.value, 1)
    val failValue = x.compareAndSet(0, 2)
    assertFalse(failValue)
    assertEquals(x.value, 1)
}
