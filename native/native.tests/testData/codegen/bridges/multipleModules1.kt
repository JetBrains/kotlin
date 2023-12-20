// MODULE: lib
// FILE: lib.kt

package a

interface A<T> {
    fun foo(): T
}

open class C: A<Int> {
    override fun foo(): Int = 42
}

// MODULE: main(lib)
// FILE: main.kt

import a.*
import kotlin.test.*

class B: C()

fun box(): String {
    val b = B()
    assertEquals(42, b.foo())
    val c: C = b
    assertEquals(42, c.foo())
    val a: A<Int> = b
    assertEquals(42, a.foo())

    return "OK"
}