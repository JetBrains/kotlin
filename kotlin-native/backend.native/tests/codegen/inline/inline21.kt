/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.inline.inline21

import kotlin.test.*

inline fun foo2(block2: () -> Int) : Int {
    println("foo2")
    return block2()
}

inline fun foo1(block1: () -> Int) : Int {
    println("foo1")
    return foo2(block1)
}

fun bar(block: () -> Int) : Int {
    println("bar")
    return foo1(block)
}

@Test fun runTest() {
    println(bar { 33 })
}
