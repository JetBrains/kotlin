/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

val sb = StringBuilder()

fun simple() {
    var a = 238
    a++
    sb.appendLine(a)
    --a
    sb.appendLine(a)
}

class Foo() {
    val j = 2
    var i = 29

    fun more() {
        i++
    }

    fun less() {
        --i
    }
}

fun fields() {
    val foo = Foo()
    foo.more()
    sb.appendLine(foo.i)
    foo.less()
    sb.appendLine(foo.i)
}

fun box(): String {
    simple()
    fields()

    assertEquals("239\n238\n30\n29\n", sb.toString())
    return "OK"
}