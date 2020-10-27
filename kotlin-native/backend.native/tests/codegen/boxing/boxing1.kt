/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.boxing.boxing1

import kotlin.test.*

fun foo(arg: Any) {
    println(arg.toString())
}

@Test fun runTest() {
    foo(1)
    foo(false)
    foo("Hello")
}