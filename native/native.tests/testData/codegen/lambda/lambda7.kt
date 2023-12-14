/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.lambda.lambda7

import kotlin.test.*

@Test fun runTest() {
    val x = foo {
        it + 1
    }
    println(x)
}

fun foo(f: (Int) -> Int) = f(42)