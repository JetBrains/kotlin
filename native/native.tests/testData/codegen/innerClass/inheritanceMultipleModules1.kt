// MODULE: lib
// FILE: lib.kt

open class Foo(val z: Int) {
    open inner class FooInner {
        fun foo() = z
    }
}

// MODULE: main(lib)
// FILE: main.kt

import kotlin.test.*

class Bar : Foo(42) {
    inner class BarInner(val x: Int) : FooInner()
}

fun box(): String {
    val o = Bar().BarInner(117)
    assertEquals(117, o.x)
    assertEquals(42, o.foo())

    return "OK"
}