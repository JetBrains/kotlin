// MODULE: base
// FILE: base.kt
package serialization.fake_overrides

open class A {
    open fun qux() = "Super"
    open fun tic() = "Super"
}

// MODULE: move(base)
// FILE: move.kt
package serialization.fake_overrides

open class X {
}

class Y: X() {
    fun bar() = "Stale"
}

class B: A() {
}

class C: A() {
    override fun tic() = "Child"
}

// MODULE: use(move, base)
// FILE: use.kt
package serialization.fake_overrides

class Z: X() {
}

fun test0() = Y().bar()
fun test2() = B().qux()
fun test3() = C().qux()

// MODULE: move2(move, base)
// FILE: move2.kt
package serialization.fake_overrides

open class X {
    fun bar() = "Moved"
}

class Y: X() {
}

class B: A() {
    override fun qux() = "Child"
}

class C: A() {
}

// MODULE: main(use, move, base)
// FILE: main.kt
import kotlin.test.assertEquals
import serialization.fake_overrides.*
fun test1() = Z().bar()

fun box(): String {
    assertEquals("Moved", test0())
    assertEquals("Moved", test1())
    assertEquals("Child", test2())
    assertEquals("Super", test3())
    return "OK"
}
