// FILE: foo.kt
package foo

fun bar(x: Int) = x

fun box(): Boolean {
    assertEquals(23, bar(23))
    assertEquals(42, foo.bar.x)
    return true
}

// FILE: foobar.kt
package foo.bar

val x = 42