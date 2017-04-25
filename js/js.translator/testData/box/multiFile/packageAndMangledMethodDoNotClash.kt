// EXPECTED_REACHABLE_NODES: 493
// FILE: foo.kt
package foo

fun bar(x: Int) = x

fun box(): String {
    assertEquals(23, bar(23))
    assertEquals(42, foo.bar.x)
    return "OK"
}

// FILE: foobar.kt
package foo.bar

val x = 42