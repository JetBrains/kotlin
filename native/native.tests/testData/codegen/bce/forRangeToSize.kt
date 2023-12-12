/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
import kotlin.test.*

fun box(): String {
    val array = Array(10) { 0L }
    val array1 = Array(3) { 0L }
    var j = 4

    assertFailsWith<IndexOutOfBoundsException> {
        for (i in 0..array.size - 1) {
            array[j] = 6
            j++
        }
    }

    var length = array.size - 1
    length = 2 * length
    assertFailsWith<IndexOutOfBoundsException> {
        for (i in 0..length) {
            array[i] = 6
        }
    }

    assertFailsWith<IndexOutOfBoundsException> {
        for (i in 0..array.size - 1) {
            array[i + 1] = 6
        }
    }

    assertFailsWith<IndexOutOfBoundsException> {
        for (i in 0..array.size - 1) {
            array1[i] = 6
        }
    }

    assertFailsWith<IndexOutOfBoundsException> {
        for (i in 0..array.size + 1) {
            array[i] = 6
        }
    }

    assertFailsWith<IndexOutOfBoundsException> {
        for (i in -1..array.size - 1) {
            array[i] = 6
        }
    }

    assertFailsWith<IndexOutOfBoundsException> {
        for (i in 0..array.size) {
            array[i] = 6
        }
    }
    return "OK"
}
