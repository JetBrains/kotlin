/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.lambda.lambda9

import kotlin.test.*

@Test fun runTest() {
    val lambdas = ArrayList<() -> Unit>()

    for (i in 0..1) {
        var x = Integer(0)
        val istr = i.toString()

        lambdas.add {
            println(istr)
            println(x.toString())
            x = x + 1
        }
    }

    val lambda1 = lambdas[0]
    val lambda2 = lambdas[1]

    lambda1()
    lambda2()
    lambda1()
    lambda2()
}

class Integer(val value: Int) {
    override fun toString() = value.toString()
    operator fun plus(other: Int) = Integer(value + other)
}