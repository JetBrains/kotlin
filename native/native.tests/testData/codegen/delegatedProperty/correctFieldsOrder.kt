// MODULE: lib
// FILE: lib.kt

package zzz

open class B {
    val z by lazy { "qzz" }
    val x = 117
    val zzz = "zzz"
}

// MODULE: main(lib)
// FILE: main.kt

import zzz.*
import kotlin.test.*

class C : B() {
    val a = "qxx"
}

fun box(): String {
    val c = C()
    assertEquals("qxx", c.a)
    assertEquals(117, c.x)
    assertEquals("zzz", c.zzz)
    assertEquals("qzz", c.z)

    return "OK"
}