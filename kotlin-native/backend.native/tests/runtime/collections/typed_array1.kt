/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

@file:OptIn(FreezingIsDeprecated::class)
package runtime.collections.typed_array1

import kotlin.test.*
import kotlin.native.concurrent.*

@Test fun runTest() {
    val array = ByteArray(17)
    val results = mutableSetOf<Any>()
    var counter = 0
    assertFailsWith<IndexOutOfBoundsException> {
        results += array.getShortAt(16)
    }
    assertFailsWith<IndexOutOfBoundsException> {
        results += array.getCharAt(22)
    }
    assertFailsWith<IndexOutOfBoundsException> {
        results += array.getIntAt(15)
    }
    assertFailsWith<IndexOutOfBoundsException> {
        results += array.getLongAt(14)
    }
    assertFailsWith<IndexOutOfBoundsException> {
        results += array.getFloatAt(14)
    }
    assertFailsWith<IndexOutOfBoundsException> {
        results += array.getDoubleAt(13)
    }
    assertFailsWith<IndexOutOfBoundsException> {
        array.setShortAt(16, 2.toShort())
    }
    assertFailsWith<IndexOutOfBoundsException>  {
        array.setCharAt(22, 'a')
    }
    assertFailsWith<IndexOutOfBoundsException>  {
        array.setIntAt(15, 1234)
    }
    assertFailsWith<IndexOutOfBoundsException> {
        array.setLongAt(14, 1.toLong())
    }
    assertFailsWith<IndexOutOfBoundsException> {
        array.setFloatAt(14, 1.0f)
    }
    assertFailsWith<IndexOutOfBoundsException> {
        array.setDoubleAt(13, 3.0)
    }
    expect(0) { results.size }

    if (Platform.isFreezingEnabled) {
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
    }
    println("OK")
}