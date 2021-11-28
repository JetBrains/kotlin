/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.function.defaultsWithVarArg2

import kotlin.test.*

fun foo(vararg arr: Int = intArrayOf(1, 2)) {
    arr.forEach { println(it) }
}

@Test fun runTest() {
    foo()
    foo(42)
}