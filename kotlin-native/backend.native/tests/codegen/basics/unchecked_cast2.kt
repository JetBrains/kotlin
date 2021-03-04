/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.basics.unchecked_cast2

import kotlin.test.*

@Test
fun runTest() {
    try {
        val x = cast<String>(Any())
        println(x.length)
    } catch (e: Throwable) {
        println("Ok")
    }
}

fun <T> cast(x: Any?) = x as T