/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

val sb = StringBuilder()

interface I {
    fun foo()
}

fun test() {
    val impl = object : I {
        override fun foo() { sb.append("zzz") }
    }

    val delegating = object: I by impl { }

    delegating.foo()
}

fun box(): String {
    test()
    assertEquals("zzz", sb.toString())

    return "OK"
}