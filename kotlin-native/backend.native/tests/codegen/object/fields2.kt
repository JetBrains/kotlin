/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.`object`.fields2

import kotlin.test.*

var global: Int = 0
    get() {
        println("Get global = $field")
        return field
    }
    set(value) {
        println("Set global = $value")
        field = value
    }

class TestClass {
    var member: Int = 0
        get() {
            println("Get member = $field")
            return field
        }
        set(value) {
            println("Set member = $value")
            field = value
        }
}

@Test fun runTest1() {
    global = 1

    val test = TestClass()
    test.member = 42

    global = test.member
    test.member = global
}

@ThreadLocal
val xInt = 42

@ThreadLocal
val xString = "42"

@ThreadLocal
val xAny = Any()

@Test fun runTest2() {
    assertEquals(42, xInt)
    assertEquals("42", xString)
    assertTrue(xAny is Any)
}
