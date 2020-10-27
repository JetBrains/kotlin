/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.branching.when9

import kotlin.test.*

@Test fun runTest() {
    foo(0)
    println("Ok")
}

fun foo(x: Int) {
    when (x) {
        0 -> 0
    }
}