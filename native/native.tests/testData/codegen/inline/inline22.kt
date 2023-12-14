/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

inline fun foo2(i2: Int): Int {
    return i2 + 3
}

inline fun foo1(i1: Int): Int {
    return foo2(i1)
}

fun bar(): Int {
    return foo1(11)
}

fun box(): String {
    assertEquals(14, bar())
    return "OK"
}
