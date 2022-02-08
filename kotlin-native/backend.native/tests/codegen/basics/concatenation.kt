/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.basics.concatenation

import kotlin.test.*

@Test
fun runTest() {
    val s = "world"
    val i = 1
    println("Hello $s $i ${2*i}")

    for (item in listOf("a", "b")) {
        println("Hello, $item")
    }
}