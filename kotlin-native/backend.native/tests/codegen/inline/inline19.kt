/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.inline.inline19

import kotlin.test.*

@Suppress("NOTHING_TO_INLINE")
inline private fun foo(i: Int): Int {
    val result = i
    return result + i
}

fun bar(): Int {
    return foo(1) + foo(2)
}

@Test fun runTest() {
    println(bar().toString())
}

