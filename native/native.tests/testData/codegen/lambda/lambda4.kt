/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

val sb = StringBuilder()

fun box(): String {
    val lambda = bar()
    lambda()
    lambda()

    assertEquals("""
        1
        2
        3
        3
        4

    """.trimIndent(), sb.toString())
    return "OK"
}

fun bar(): () -> Unit {
    var x = Integer(0)

    val lambda = {
        sb.appendLine(x.toString())
        x = x + 1
    }

    x = x + 1

    lambda()
    lambda()

    sb.appendLine(x.toString())

    return lambda
}

class Integer(val value: Int) {
    override fun toString() = value.toString()
    operator fun plus(other: Int) = Integer(value + other)
}