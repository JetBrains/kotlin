/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
import kotlin.test.*

fun foo(a: Int, b : Int): Int = a + b * 2

fun box(): String {
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

    if (array.toList() != listOf(7, 7, 7, 7, 7, 7, 7, 7, 7, 7)) return "FAIL 1: ${array.toList()}"
    if (array1.toList() != listOf(7, 7, 7)) return "FAIL 2: ${array1.toList()}"

    return "OK"
}
