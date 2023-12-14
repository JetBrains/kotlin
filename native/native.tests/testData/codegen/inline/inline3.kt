/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

@Suppress("NOTHING_TO_INLINE")
inline fun foo(i4: Int, i5: Int): Int {
    try {
        return i4 / i5
    } catch (e: Throwable) {
        return i4
    }
}

fun bar(i1: Int, i2: Int, i3: Int): Int {
    return i1 + foo(i2, i3)
}

fun box(): String {
    assertEquals(5, bar(1, 8, 2))
    return "OK"
}
