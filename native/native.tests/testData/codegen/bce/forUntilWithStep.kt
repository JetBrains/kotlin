/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
import kotlin.test.*

fun box(): String {
    val array = CharArray(10) { '0' }
    val array1 = CharArray(3) { '0' }
    var j = 8

    assertFailsWith<IndexOutOfBoundsException> {
        for (i in 0 until array.size step 2) {
            array[j] = '6'
            j++
        }
    }

    assertFailsWith<IndexOutOfBoundsException> {
        for (i in 0 until array.size step 2) {
            array[i + 3] = '6'
        }
    }

    assertFailsWith<IndexOutOfBoundsException> {
        for (i in 0 until array.size step 2) {
            array1[i] = '6'
        }
    }

    assertFailsWith<IndexOutOfBoundsException> {
        for (i in 0 until (array.size/0.5).toInt() step 2) {
            array[i] = '6'
        }
    }

    assertFailsWith<IndexOutOfBoundsException> {
        for (i in -array.size until array.size step 2) {
            array[i] = '6'
        }
    }
    return "OK"
}
