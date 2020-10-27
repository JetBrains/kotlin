/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.lambda.lambda6

import kotlin.test.*

@Test fun runTest() {
    val str = "captured"
    foo {
        println(it)
        println(str)
    }
}

fun foo(f: (Int) -> Unit) {
    f(42)
}