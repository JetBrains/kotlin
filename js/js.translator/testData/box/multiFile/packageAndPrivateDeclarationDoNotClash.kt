// FILE: foo.kt
package foo

private fun bar() = 23

private val bar = 42

fun box(): Boolean {
    assertEquals(23, bar())
    assertEquals(42, bar)
    assertEquals(32, foo.bar.x)

    return true
}

// FILE: foobar.kt
package foo.bar

val x = 32
