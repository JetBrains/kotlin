/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

fun box(): String {
    val list = foo()
    assertTrue(list === foo())
    assertEquals("[a, b, c]", list.toString())
    return "OK"
}

fun foo(): List<String> {
    return listOf("a", "b", "c")
}