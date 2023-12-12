/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

fun <T> areSame(arg1: T, arg2: T): Boolean {
    return arg1 === arg2
}

fun Boolean.oneIfTrueElseZero(): Int {
    return if (this) 1 else 0
}

fun box(): String {
    var acc = 0
    val range = 1000

    for (i in arrayOf(false, true)) {
        for (j in arrayOf(false, true)) {
            acc += areSame(i, j).oneIfTrueElseZero()
        }
    }
    if (acc != 2) "FAIL 1: $acc"

    acc = 0
    for (i in Byte.MIN_VALUE..Byte.MAX_VALUE) {
        acc += areSame(i, i).oneIfTrueElseZero()
    }
    if (acc != 256) "FAIL 2: $acc"

    acc = 0
    for (i in Short.MIN_VALUE..Short.MAX_VALUE) {
        acc += areSame(i, i).oneIfTrueElseZero()
    }
    if (acc != 256) "FAIL 3: $acc"

    acc = 0
    for (i in 0.toChar()..range.toChar()) {
        acc += areSame(i, i).oneIfTrueElseZero()
    }
    if (acc != 256) "FAIL 4: $acc"

    acc = 0
    for (i in -range..range) {
        acc += areSame(i, i).oneIfTrueElseZero()
    }
    if (acc != 256) "FAIL 5: $acc"

    acc = 0
    for (i in -range.toLong()..range.toLong()) {
        acc += areSame(i, i).oneIfTrueElseZero()
    }
    if (acc != 256) "FAIL 6: $acc"

    return "OK"
}