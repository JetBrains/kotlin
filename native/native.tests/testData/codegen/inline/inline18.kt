/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

@Suppress("NOTHING_TO_INLINE")
inline fun <T> foo2(i2: T, j2: T, p2: (T, T) -> Boolean): Boolean {
    return p2(i2, j2)
}

@Suppress("NOTHING_TO_INLINE")
inline fun <T> foo1(i1: T, j1: T, p1: (T, T) -> Boolean): Boolean {
    return foo2<T>(i1, j1, p1)
}

fun bar(): Boolean {
    val result = foo1 <Int> (3, 3) { x1: Int, x2: Int -> x1 == x2 }
    return result
}

fun box(): String {
    assertTrue(bar())
    return "OK"
}
