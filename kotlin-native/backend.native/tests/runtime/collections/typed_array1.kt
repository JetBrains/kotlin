/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package runtime.collections.typed_array1

import kotlin.test.*
import kotlin.native.concurrent.*

@Test fun runTest() {
    val array = ByteArray(17)
    val results = mutableSetOf<Any>()
    var counter = 0
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        results += array.getShortAt(16)
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        results += array.getCharAt(22)
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        results += array.getIntAt(15)
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        results += array.getLongAt(14)
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        results += array.getFloatAt(14)
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        results += array.getDoubleAt(13)
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        array.setShortAt(16, 2.toShort())
    }
    assertFailsWith<ArrayIndexOutOfBoundsException>  {
        array.setCharAt(22, 'a')
    }
    assertFailsWith<ArrayIndexOutOfBoundsException>  {
        array.setIntAt(15, 1234)
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        array.setLongAt(14, 1.toLong())
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        array.setFloatAt(14, 1.0f)
    }
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        array.setDoubleAt(13, 3.0)
    }
    expect(0) { results.size }

    array.freeze()
    assertFailsWith<InvalidMutabilityException> {
        array.setShortAt(0, 2.toShort())
    }
    assertFailsWith<InvalidMutabilityException> {
        array.setCharAt(0, 'a')
    }
    assertFailsWith<InvalidMutabilityException> {
        array.setIntAt(0, 2)
    }
    assertFailsWith<InvalidMutabilityException> {
        array.setLongAt(0, 2)
    }
    assertFailsWith<InvalidMutabilityException> {
        array.setFloatAt(0, 1.0f)
    }
    assertFailsWith<InvalidMutabilityException> {
        array.setDoubleAt(0, 1.0)
    }
    println("OK")
}