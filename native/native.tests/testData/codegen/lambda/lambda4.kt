// OUTPUT_DATA_FILE: lambda4.out
/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */


import kotlin.test.*

fun box(): String {
    val lambda = bar()
    lambda()
    lambda()

    return "OK"
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
