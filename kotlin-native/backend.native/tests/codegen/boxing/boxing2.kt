/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.boxing.boxing2

import kotlin.test.*

fun printInt(x: Int) = println(x)
fun printBoolean(x: Boolean) = println(x)

fun foo(arg: Any) {
    if (arg is Int)
        printInt(arg)
    else if (arg is Boolean)
        printBoolean(arg)
    else
        println("other")
}

@Test fun runTest() {
    foo(1)
    foo(true)
    foo("Hello")
}