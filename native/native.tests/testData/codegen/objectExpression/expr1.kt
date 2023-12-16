/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

fun box(): String {
    val a = "a"

    val x = object {
        override fun toString(): String {
            return foo(a) + foo("b")
        }

        fun foo(s: String) = s + s
    }

    assertEquals("aabb", x.toString())
    return "OK"
}