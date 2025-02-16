/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
// IGNORE_NATIVE: optimizationMode=DEBUG
// IGNORE_NATIVE: optimizationMode=NO

import kotlin.test.*
import kotlin.native.internal.*

class C(val x: Int)

fun stackArray1(x: Int): Int {
    val c1 = C(x + 42)
    val c2 = C(x + 117)
    val c3 = C(x - 1)
    val allC = arrayOf(c1, c2, c3)
    val array = Array(10) { allC[it % 3] }

    assertTrue(c1.isStack())
    assertTrue(c2.isStack())
    assertTrue(c3.isStack())
    assertTrue(allC.isStack())
    assertTrue(array.isStack())
    return array[5].x
}

fun stackArray2(x: Int): Int {
    val c1 = C(x + 42)
    val c2 = C(x + 117)
    val c3 = C(x - 1)
    val allC = arrayOf(c1, c2, c3)
    val size = 42
    val a = size
    val b = if (x > 10) size else a
    val array = Array(b) { allC[it % 3] }

    assertTrue(c1.isStack())
    assertTrue(c2.isStack())
    assertTrue(c3.isStack())
    assertTrue(allC.isStack())
    assertTrue(array.isStack())
    return array[5].x
}

fun localArray(x: Int): Int {
    val c1 = C(x + 42)
    val c2 = C(x + 117)
    val c3 = C(x - 1)
    val allC = arrayOf(c1, c2, c3)
    val array = Array(x) { allC[it % 3] }

    assertTrue(c1.isStack())
    assertTrue(c2.isStack())
    assertTrue(c3.isStack())
    assertTrue(allC.isStack())
    assertFalse(array.isStack())
    return array[5].x
}

fun box(): String {
    assertEquals(stackArray1(43), 42)
    assertEquals(stackArray2(43), 42)
    assertEquals(localArray(43), 42)

    return "OK"
}
