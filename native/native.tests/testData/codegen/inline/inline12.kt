/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

@Suppress("NOTHING_TO_INLINE")
inline fun <T> foo (): Boolean {
    return Any() is Any
}

fun bar(i1: Int): Boolean {
    return foo<Double>()
}

fun box(): String {
    assertTrue(bar(1))
    return "OK"
}
