/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.inline.inline6

import kotlin.test.*

@Suppress("NOTHING_TO_INLINE")
inline fun foo(body: () -> Unit) {
    println("hello1")
    body()
    println("hello4")
}

fun bar() {
    foo {
        println("hello2")
        println("hello3")
    }
}

@Test fun runTest() {
    bar()
}
