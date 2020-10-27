/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.basics.expression_as_statement

import kotlin.test.*

fun foo() {
    Any() as String
}

@Test
fun runTest() {
    try {
        foo()
    } catch (e: Throwable) {
        println("Ok")
        return
    }

    println("Fail")
}