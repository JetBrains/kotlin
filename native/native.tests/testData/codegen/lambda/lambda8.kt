/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

val sb = StringBuilder()

fun box(): String {
    val lambda1 = bar("first")
    val lambda2 = bar("second")

    lambda1()
    lambda2()
    lambda1()
    lambda2()

    assertEquals("""
        first
        0
        second
        0
        first
        1
        second
        1

    """.trimIndent(), sb.toString())
    return "OK"
}

fun bar(str: String): () -> Unit {
    var x = Integer(0)

    return {
        sb.appendLine(str)
        sb.appendLine(x.toString())
        x = x + 1
    }
}

class Integer(val value: Int) {
    override fun toString() = value.toString()
    operator fun plus(other: Int) = Integer(value + other)
}