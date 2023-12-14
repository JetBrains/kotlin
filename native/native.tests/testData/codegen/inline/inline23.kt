/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

inline fun <reified T> foo(i2: Any): T {
    return i2 as T
}

fun bar(i1: Int): Int {
    return foo<Int>(i1)
}

fun box(): String {
    assertEquals(33, bar(33))
    return "OK"
}
