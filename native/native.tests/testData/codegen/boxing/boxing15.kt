/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

fun box(): String {
    val res1 = foo(17)
    if (res1 != 17) return "FAIL 1: $res1"

    val nonConst = 17
    val res2 = foo(nonConst)
    if (res2 != 17) return "FAIL 2: $res2"

    return "OK"
}

fun <T : Int> foo(x: T): Int = x