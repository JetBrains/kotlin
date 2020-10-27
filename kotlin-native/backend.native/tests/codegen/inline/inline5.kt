/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.inline.inline5

import kotlin.test.*

@Suppress("NOTHING_TO_INLINE")
inline fun foo(i2: Int, body: () -> Int): Int {
    return i2 + body()
}

fun bar(i1: Int): Int {
    return foo(i1) { return 33 }
}

@Test fun runTest() {
    println(bar(1).toString())
}

