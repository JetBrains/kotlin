/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

fun sum3():Int = sum(1, 2, 33)
fun sum(a:Int, b:Int, c:Int): Int = a + b + c

fun box(): String {
    assertEquals(36, sum3())
    return "OK"
}