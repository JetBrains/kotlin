// MODULE: lib
// FILE: lib.kt

package zzz

interface I {
    fun foo(): Int
}

open class A : I {
    override fun foo() = 42
}

open class B : I by A() {
    val x = 117
    val y = "zzz"
}

// MODULE: main(lib)
// FILE: main.kt

import zzz.*
import kotlin.test.*

class C : B() {
    val a = "qxx"
    val b = 123
}

fun box(): String {
    val c = C()
    assertEquals("qxx", c.a)
    assertEquals(123, c.b)
    assertEquals(42, c.foo())
    assertEquals(117, c.x)
    assertEquals("zzz", c.y)

    return "OK"
}