/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
import kotlin.test.*

fun box(): String {
    val array = Array(10) { 100 }
    val array1 = Array(3) { 0 }
    var j = 8

    assertFailsWith<IndexOutOfBoundsException> {
        for ((index, value) in array.withIndex()) {
            array[j] = 6
            j++
        }
    }

    assertFailsWith<IndexOutOfBoundsException> {
        for ((index, value) in array.withIndex()) {
            array[index + 1] = 6
        }
    }

    assertFailsWith<IndexOutOfBoundsException> {
        for ((index, value) in array.withIndex()) {
            array[value] = 6
        }
    }

    assertFailsWith<IndexOutOfBoundsException> {
        for ((i, v) in (0..array.size + 30 step 2).withIndex()) {
            array[i] = 6
        }
    }

    assertFailsWith<IndexOutOfBoundsException> {
        for ((i, v) in (0..array.size).withIndex()) {
            array[v] = 8
        }
    }
    return "OK"
}
