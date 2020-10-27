/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.lambda.lambda4

import kotlin.test.*

@Test fun runTest() {
    val lambda = bar()
    lambda()
    lambda()
}

fun bar(): () -> Unit {
    var x = Integer(0)

    val lambda = {
        println(x.toString())
        x = x + 1
    }

    x = x + 1

    lambda()
    lambda()

    println(x.toString())

    return lambda
}

class Integer(val value: Int) {
    override fun toString() = value.toString()
    operator fun plus(other: Int) = Integer(value + other)
}