/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package codegen.bce.arraysForLoops

import kotlin.test.*

@Test fun forEachIndexedTest() {
    val array = Array(10) { 0 }

    assertFailsWith<ArrayIndexOutOfBoundsException> {
        array.forEachIndexed { index, _ ->
            array[index + 1] = 1
        }
    }
}

@Test fun forEachIndicies() {
    val array = Array(10) { 0 }
    val array1 = Array(3) { 0 }
    var j = 4

    assertFailsWith<ArrayIndexOutOfBoundsException> {
        for (i in array.indices) {
            array[j] = 6
            j++
        }
    }

    assertFailsWith<ArrayIndexOutOfBoundsException> {
        for (i in array.indices) {
            array[i + 1] = 6
        }
    }

    assertFailsWith<ArrayIndexOutOfBoundsException> {
        for (i in array.indices) {
            array1[i] = 6
        }
    }
}

@Test fun forUntilSize() {
    val array = Array(10) { 0L }
    val array1 = Array(3) { 0L }
    var j = 4

    assertFailsWith<ArrayIndexOutOfBoundsException> {
        for (i in 0 until array.size) {
            array[j] = 6
            j++
        }
    }

    assertFailsWith<ArrayIndexOutOfBoundsException> {
        for (i in 0 until array.size) {
            array[i - 1] = 6
        }
    }

    assertFailsWith<ArrayIndexOutOfBoundsException> {
        for (i in 0 until array.size) {
            array1[i] = 6
        }
    }

    assertFailsWith<ArrayIndexOutOfBoundsException> {
        for (i in 0 until array.size + 10) {
            array[i] = 6
        }
    }
}

@Test fun forDownToSize() {
    val array = Array(10) { 0L }
    val array1 = Array(3) { 0L }
    var j = 4

    assertFailsWith<ArrayIndexOutOfBoundsException> {
        for (i in array.size - 1 downTo 0) {
            array[j] = 6
            j++
        }
    }

    assertFailsWith<ArrayIndexOutOfBoundsException> {
        for (i in array.size - 1 downTo 0) {
            array[i *  2] = 6
        }
    }

    assertFailsWith<ArrayIndexOutOfBoundsException> {
        for (i in array.size - 1 downTo 0) {
            array1[i] = 6
        }
    }

    var a = array.size - 1
    val b = ++a
    val c = b

    assertFailsWith<ArrayIndexOutOfBoundsException> {
        for (i in c downTo 0) {
            array[i] = 6
        }
    }

    assertFailsWith<ArrayIndexOutOfBoundsException> {
        for (i in array.size + 1 downTo 0) {
            array[i] = 6
        }
    }

    assertFailsWith<ArrayIndexOutOfBoundsException> {
        for (i in array.size - 1 downTo -1) {
            array[i] = 6
        }
    }

    assertFailsWith<ArrayIndexOutOfBoundsException> {
        for (i in array.size downTo 0) {
            array[i] = 6
        }
    }
}

@Test fun forRangeToSize() {
    val array = Array(10) { 0L }
    val array1 = Array(3) { 0L }
    var j = 4

    assertFailsWith<ArrayIndexOutOfBoundsException> {
        for (i in 0..array.size - 1) {
            array[j] = 6
            j++
        }
    }

    var length = array.size - 1
    length = 2 * length
    assertFailsWith<ArrayIndexOutOfBoundsException> {
        for (i in 0..length) {
            array[i] = 6
        }
    }

    assertFailsWith<ArrayIndexOutOfBoundsException> {
        for (i in 0..array.size - 1) {
            array[i + 1] = 6
        }
    }

    assertFailsWith<ArrayIndexOutOfBoundsException> {
        for (i in 0..array.size - 1) {
            array1[i] = 6
        }
    }

    assertFailsWith<ArrayIndexOutOfBoundsException> {
        for (i in 0..array.size + 1) {
            array[i] = 6
        }
    }

    assertFailsWith<ArrayIndexOutOfBoundsException> {
        for (i in -1..array.size - 1) {
            array[i] = 6
        }
    }

    assertFailsWith<ArrayIndexOutOfBoundsException> {
        for (i in 0..array.size) {
            array[i] = 6
        }
    }
}

@Test fun forRangeToWithStep() {
    val array = Array(10) { 0L }
    val array1 = Array(3) { 0L }
    var j = 8

    assertFailsWith<ArrayIndexOutOfBoundsException> {
        for (i in 0..array.size - 1 step 2) {
            array[j] = 6
            j++
        }
    }

    assertFailsWith<ArrayIndexOutOfBoundsException> {
        for (i in 0..array.size - 1 step 2) {
            array[i - 1] = 6
        }
    }

    assertFailsWith<ArrayIndexOutOfBoundsException> {
        for (i in 0..array.size - 1 step 2) {
            array1[i] = 6
        }
    }

    assertFailsWith<ArrayIndexOutOfBoundsException> {
        for (i in 0..array.size + 1 step 2) {
            array[i] = 6
        }
    }

    assertFailsWith<ArrayIndexOutOfBoundsException> {
        for (i in -1..array.size - 1 step 2) {
            array[i] = 6
        }
    }

    assertFailsWith<ArrayIndexOutOfBoundsException> {
        for (i in 0..array.size step 2) {
            array[i] = 6
        }
    }
}

@Test fun forUntilWithStep() {
    val array = CharArray(10) { '0' }
    val array1 = CharArray(3) { '0' }
    var j = 8

    assertFailsWith<ArrayIndexOutOfBoundsException> {
        for (i in 0 until array.size step 2) {
            array[j] = '6'
            j++
        }
    }

    assertFailsWith<ArrayIndexOutOfBoundsException> {
        for (i in 0 until array.size step 2) {
            array[i + 3] = '6'
        }
    }

    assertFailsWith<ArrayIndexOutOfBoundsException> {
        for (i in 0 until array.size step 2) {
            array1[i] = '6'
        }
    }

    assertFailsWith<ArrayIndexOutOfBoundsException> {
        for (i in 0 until (array.size/0.5).toInt() step 2) {
            array[i] = '6'
        }
    }

    assertFailsWith<ArrayIndexOutOfBoundsException> {
        for (i in -array.size until array.size step 2) {
            array[i] = '6'
        }
    }
}

@Test fun forDownToWithStep() {
    val array = Array(10) { 0L }
    val array1 = Array(3) { 0L }
    var j = 8

    assertFailsWith<ArrayIndexOutOfBoundsException> {
        for (i in array.size - 1 downTo 0 step 2) {
            array[j] = 6
            j++
        }
    }

    assertFailsWith<ArrayIndexOutOfBoundsException> {
        for (i in array.size - 1 downTo 1 step 2) {
            array[i + 1] = 6
        }
    }

    assertFailsWith<ArrayIndexOutOfBoundsException> {
        for (i in array.size - 1 downTo 1 step 2) {
            array1[i] = 6
        }
    }

    assertFailsWith<ArrayIndexOutOfBoundsException> {
        for (i in (array.size / 0.2).toInt() downTo 1 step 2) {
            array[i] = 6
        }
    }

    assertFailsWith<ArrayIndexOutOfBoundsException> {
        for (i in array.size - 1 downTo -3 step 2) {
            array[i] = 6
        }
    }

    assertFailsWith<ArrayIndexOutOfBoundsException> {
        for (i in array.size downTo 1 step 2) {
            array[i] = 6
        }
    }
}

@Test fun forIndiciesWithStep() {
    val array = Array(10) { 0L }
    val array1 = Array(3) { 0L }
    var j = 8

    assertFailsWith<ArrayIndexOutOfBoundsException> {
        for (i in array.indices step 2) {
            array[j] = 6
            j++
        }
    }

    assertFailsWith<ArrayIndexOutOfBoundsException> {
        for (i in array.indices step 2) {
            array[i - 1] = 6
        }
    }

    assertFailsWith<ArrayIndexOutOfBoundsException> {
        for (i in array.indices step 2) {
            array1[i] = 6
        }
    }
}

@Test fun forWithIndex() {
    val array = Array(10) { 100 }
    val array1 = Array(3) { 0 }
    var j = 8

    assertFailsWith<ArrayIndexOutOfBoundsException> {
        for ((index, value) in array.withIndex()) {
            array[j] = 6
            j++
        }
    }

    assertFailsWith<ArrayIndexOutOfBoundsException> {
        for ((index, value) in array.withIndex()) {
            array[index + 1] = 6
        }
    }

    assertFailsWith<ArrayIndexOutOfBoundsException> {
        for ((index, value) in array.withIndex()) {
            array[value] = 6
        }
    }

    assertFailsWith<ArrayIndexOutOfBoundsException> {
        for ((i, v) in (0..array.size + 30 step 2).withIndex()) {
            array[i] = 6
        }
    }

    assertFailsWith<ArrayIndexOutOfBoundsException> {
        for ((i, v) in (0..array.size).withIndex()) {
            array[v] = 8
        }
    }
}

@Test fun forReversed() {
    val array = Array(10) { 100 }
    val array1 = Array(3) { 0 }
    var j = 8

    assertFailsWith<ArrayIndexOutOfBoundsException> {
        for (i in (0..array.size-1).reversed()) {
            array[j] = 6
            j++
        }
    }

    assertFailsWith<ArrayIndexOutOfBoundsException> {
        for (i in (0 until array.size).reversed()) {
            array1[i] = 6
        }
    }

    assertFailsWith<ArrayIndexOutOfBoundsException> {
        for (i in (0..array.size).reversed()) {
            array[i] = 6
        }
    }

    assertFailsWith<ArrayIndexOutOfBoundsException> {
        for (i in (array.size downTo 0).reversed()) {
            array[i] = 6
        }
    }
}

fun foo(a: Int, b : Int): Int = a + b * 2

@Test fun bceCases() {
    val array = Array(10) { 100 }
    val array1 = Array(3) { 0 }
    var length = array.size  - 1
    var sum = 0

    array.forEach {
        sum += it
    }

    for (i in array.indices) {
        array[i] = 6
    }

    for (i in 0 until array.size) {
        array[i] = 7
    }

    for (i in array.size - 1 downTo 1) {
        array[i] = 7
    }

    for (it in array) {
        sum += it
    }

    for (i in 0..array.size - 1 step 2) {
        array[i] = 7
    }

    for (i in 0 until array.size step 2) {
        array[i] = 7
    }

    for (i in array.indices step 2) {
        array[i] = 6
    }

    for (i in array.size - 1 downTo 1 step 2) {
        array[i] = 7
    }

    for ((index, value) in array.withIndex()) {
        array[index] = 8
    }

    for ((i, v) in (0..array.size - 1 step 2).withIndex()) {
        array[v] = 8
        array[i] = 6
    }
    for (i in array.reversed()) {
        sum += i
    }

    for (i in (0..array.size-1).reversed()) {
        array [i] = 10
    }

    for (i in 0 until array.size) {
        array[i] = 7
        for (j in 0 until array1.size) {
            array1[j] = array[i]
        }
    }

    val size = array.size - 1
    val size1 = size

    for (i in 0..size1) {
        foo(array[i], array[i])
    }

    for (i in 0..array.size - 2) {
        array[i+1] = array[i]
    }
}