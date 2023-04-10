/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

import a.*

fun main() {
    try {
        val res = foo(intArrayOf(1, 2, 3)) { x, y, z -> x + y - z }
        println(res)
    } catch (t: UninitializedPropertyAccessException) {
        println("OK")
    }
}