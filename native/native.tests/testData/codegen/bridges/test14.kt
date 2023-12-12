/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

val sb = StringBuilder()

open class A<T, U> {
    open fun foo(x: T, y: U) {
        sb.appendLine(x.toString())
        sb.appendLine(y.toString())
    }
}

interface I1<T> {
    fun foo(x: Int, y: T)
}

interface I2<T> {
    fun foo(x: T, y: Int)
}

class B : A<Int, Int>(), I1<Int>, I2<Int> {
    var z: Int = 5
    var q: Int = 7
    override fun foo(x: Int, y: Int) {
        z = x
        q = y
    }
}

fun zzz(a: A<Int, Int>) {
    a.foo(42, 56)
}

fun box(): String {
    val b = B()
    zzz(b)
    val a = A<Int, Int>()
    zzz(a)
    sb.appendLine(b.z.toString())
    sb.appendLine(b.q.toString())
    val i1: I1<Int> = b
    i1.foo(56, 42)
    sb.appendLine(b.z.toString())
    sb.appendLine(b.q.toString())
    val i2: I2<Int> = b
    i2.foo(156, 142)
    sb.appendLine(b.z.toString())
    sb.appendLine(b.q.toString())

    assertEquals("""
        42
        56
        42
        56
        56
        42
        156
        142

    """.trimIndent(), sb.toString())
    return "OK"
}