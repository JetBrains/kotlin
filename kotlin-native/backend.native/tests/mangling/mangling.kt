/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

fun test_direct() {
    val mutListInt = mutableListOf<Int>(1, 2, 3, 4)
    val mutListNum = mutableListOf<Number>(9, 10, 11, 12)
    val mutListAny = mutableListOf<Any>(5, 6, 7, 8)

    mangle1(mutListInt)
    mangle1(mutListNum)
    mangle1(mutListAny)
}

fun test_param() {
    val mutListInt = mutableListOf<Int>(1, 2, 3, 4)
    val mutListNum = mutableListOf<Number>(9, 10, 11, 12)
    val mutListAny = mutableListOf<Any>(5, 6, 7, 8)

    mangle2(mutListInt)
    mangle2(mutListNum)
    mangle2(mutListAny)
}

fun test_multiple_constructors() {
    val any = mapOf<Float, Float>()
    val comparable = "some string"
    val number = 17

    mangle3(any)
    mangle3(comparable)
    mangle3(number)
}

fun main(args: Array<String>) {
    test_direct()
    test_param()
    test_multiple_constructors()
}

