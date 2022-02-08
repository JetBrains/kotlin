/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.objectExpression.expr2
import kotlin.test.*

@Test fun runTest() {
    val a = "a"

    val x = object {
        override fun toString(): String {
            return foo {
                a
            }
        }

        fun foo(lambda: () -> String) = lambda()
    }

    print(x)
}

fun print(x: Any) = println(x.toString())