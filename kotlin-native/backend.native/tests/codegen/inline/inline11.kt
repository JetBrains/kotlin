/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.inline.inline11

import kotlin.test.*

@Suppress("NOTHING_TO_INLINE")
inline fun <reified T> foo (i2: Any): Boolean {
    return i2 is T
}

fun bar(i1: Int): Boolean {
    return foo<Double>(i1)
}

@Test fun runTest() {
    println(bar(1).toString())
}
