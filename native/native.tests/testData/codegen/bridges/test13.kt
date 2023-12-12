/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

val sb = StringBuilder()

open class A<T> {
    open fun T.foo() {
        sb.appendLine(this.toString())
    }

    fun bar(x: T) {
        x.foo()
    }
}

open class B: A<Int>() {
    override fun Int.foo() {
        sb.appendLine(this.toString())
    }
}

fun box(): String {
    val b = B()
    val a = A<Int>()
    b.bar(42)
    a.bar(42)

    assertEquals("42\n42\n", sb.toString())
    return "OK"
}