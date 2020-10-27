/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.basics.unit3

import kotlin.test.*

@Test
fun runTest() {
    foo(Unit)
}

fun foo(x: Any) {
    println(x.toString())
}