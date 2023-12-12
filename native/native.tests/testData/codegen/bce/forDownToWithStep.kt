/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
import kotlin.test.*

fun box(): String {
    val array = UIntArray(10) { 0U }
    val array1 = UIntArray(3) { 0U }
    var j = 8

    assertFailsWith<IndexOutOfBoundsException> {
        for (i in array.size - 1 downTo 0 step 2) {
            array[j] = 6U
            j++
        }
    }

    assertFailsWith<IndexOutOfBoundsException> {
        for (i in array.size - 1 downTo 1 step 2) {
            array[i + 1] = 6U
        }
    }

    assertFailsWith<IndexOutOfBoundsException> {
        for (i in array.size - 1 downTo 1 step 2) {
            array1[i] = 6U
        }
    }

    assertFailsWith<IndexOutOfBoundsException> {
        for (i in (array.size / 0.2).toInt() downTo 1 step 2) {
            array[i] = 6U
        }
    }

    assertFailsWith<IndexOutOfBoundsException> {
        for (i in array.size - 1 downTo -3 step 2) {
            array[i] = 6U
        }
    }

    assertFailsWith<IndexOutOfBoundsException> {
        for (i in array.size downTo 1 step 2) {
            array[i] = 6U
        }
    }
    return "OK"
}
