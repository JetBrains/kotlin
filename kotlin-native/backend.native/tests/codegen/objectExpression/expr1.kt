/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.objectExpression.expr1

import kotlin.test.*

@Test fun runTest() {
    val a = "a"

    val x = object {
        override fun toString(): String {
            return foo(a) + foo("b")
        }

        fun foo(s: String) = s + s
    }

    println(x.toString())
}