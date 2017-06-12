// EXPECTED_REACHABLE_NODES: 494
// FILE: foo.kt
package foo

private fun bar() = 23

private val bar = 42

fun box(): String {
    assertEquals(23, bar())
    assertEquals(42, bar)
    assertEquals(32, foo.bar.x)

    return "OK"
}

// FILE: foobar.kt
package foo.bar

val x = 32
