/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.boxing.boxing13

import kotlin.test.*

fun is42(x: Any?) {
    println(x == 42)
    println(42 == x)
}

@Test fun runTest() {
    is42(16)
    is42(42)
    is42("42")
}