/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

@Suppress("NOTHING_TO_INLINE")
inline fun foo(i4: Int, i5: Int): Int {
    if (i4 > 0) return i4
    return i5
}

fun bar(i1: Int, i2: Int): Int {
    return foo(i1, i2)
}

fun box(): String {
    assertEquals(3, bar(3, 8))
    return "OK"
}
