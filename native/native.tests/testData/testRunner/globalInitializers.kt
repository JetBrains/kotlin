// FILE: lib.kt
var z1 = false
var z2 = false
var t1 = false
var t2 = false

// FILE: lib2.kt
import kotlin.test.*

private val x = foo()

private fun foo(): Int {
    z1 = true
    return 42
}

@Test
fun test1() {
    t1 = true
    assertTrue(z1)
    assertTrue(t2 || !z2)
}

// FILE: main.kt
import kotlin.test.*

private val x = foo()

private fun foo(): Int {
    z2 = true
    return 42
}

@Test
fun test2() {
    t2 = true
    assertTrue(z2)
    assertTrue(t1 || !z1)
}