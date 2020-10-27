/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.boxing.boxing5

import kotlin.test.*

fun printInt(x: Int) = println(x)

fun foo(arg: Int?) {
    printInt(arg ?: 16)
}

@Test fun runTest() {
    foo(null)
    foo(42)
}