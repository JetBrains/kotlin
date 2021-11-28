/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package runtime.collections.array4

import kotlin.test.*

@Test fun runTest() {
    assertFailsWith<IllegalArgumentException> {
        val a = Array(-2) { "nope" }
        println(a)
    }
    assertFailsWith<IllegalArgumentException> {
        val a = ByteArray(-2)
        println(a)
    }
    assertFailsWith<IllegalArgumentException> {
        val a = UByteArray(-2)
        println(a)
    }
    assertFailsWith<IllegalArgumentException> {
        val a = ShortArray(-2)
        println(a)
    }
    assertFailsWith<IllegalArgumentException> {
        val a = UShortArray(-2)
        println(a)
    }
    assertFailsWith<IllegalArgumentException> {
        val a = IntArray(-2)
        println(a)
    }
    assertFailsWith<IllegalArgumentException> {
        val a = UIntArray(-2)
        println(a)
    }
    assertFailsWith<IllegalArgumentException> {
        val a = LongArray(-2)
        println(a)
    }
    assertFailsWith<IllegalArgumentException> {
        val a = ULongArray(-2)
        println(a)
    }
    assertFailsWith<IllegalArgumentException> {
        val a = FloatArray(-2)
        println(a)
    }
    assertFailsWith<IllegalArgumentException> {
        val a = DoubleArray(-2)
        println(a)
    }
    assertFailsWith<IllegalArgumentException> {
        val a = BooleanArray(-2)
        println(a)
    }
    assertFailsWith<IllegalArgumentException> {
        val a = CharArray(-2)
        println(a)
    }
    println("OK")
}