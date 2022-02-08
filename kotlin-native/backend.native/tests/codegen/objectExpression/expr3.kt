/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.objectExpression.expr3

import kotlin.test.*

@Test fun runTest() {
    var cnt = 0

    var x: Any = ""

    for (i in 0 .. 1) {
        print(x)
        cnt++
        val y = object {
            override fun toString() = cnt.toString()
        }
        x = y
    }
    print(x)
}

fun print(x: Any) = println(x.toString())