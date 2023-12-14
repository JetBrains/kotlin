/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.lambda.lambda8

import kotlin.test.*

@Test fun runTest() {
    val lambda1 = bar("first")
    val lambda2 = bar("second")

    lambda1()
    lambda2()
    lambda1()
    lambda2()
}

fun bar(str: String): () -> Unit {
    var x = Integer(0)

    return {
        println(str)
        println(x.toString())
        x = x + 1
    }
}

class Integer(val value: Int) {
    override fun toString() = value.toString()
    operator fun plus(other: Int) = Integer(value + other)
}