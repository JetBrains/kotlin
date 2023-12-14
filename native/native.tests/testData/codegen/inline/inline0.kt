/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

@Suppress("NOTHING_TO_INLINE")
inline fun foo(i1: Int, j1: Int): Int {
    return i1 + j1
}

fun bar(i: Int, j: Int): Int {
    return i + foo(i, j)
}

fun box(): String {
    assertEquals(84, bar(41, 2))
    return "OK"
}

