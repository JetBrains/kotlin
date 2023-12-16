/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

val sb = StringBuilder()

var global: Int = 0
    get() {
        sb.appendLine("Get global = $field")
        return field
    }
    set(value) {
        sb.appendLine("Set global = $value")
        field = value
    }

class TestClass {
    var member: Int = 0
        get() {
            sb.appendLine("Get member = $field")
            return field
        }
        set(value) {
            sb.appendLine("Set member = $value")
            field = value
        }
}

fun box(): String {
    global = 1

    val test = TestClass()
    test.member = 42

    global = test.member
    test.member = global

    assertEquals("""
        Set global = 1
        Set member = 42
        Get member = 42
        Set global = 42
        Get global = 42
        Set member = 42

    """.trimIndent(), sb.toString())
    return "OK"
}
