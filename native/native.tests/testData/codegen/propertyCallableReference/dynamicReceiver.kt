/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

val sb = StringBuilder()

class TestClass {
    var x: Int = 42
}

fun foo(): TestClass {
    sb.append(42)
    return TestClass()
}

fun box(): String {
    foo()::x

    assertEquals("42", sb.toString())
    return "OK"
}