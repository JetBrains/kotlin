// EXPECTED_REACHABLE_NODES: 491
// FILE: a.kt
package foo

inline fun sum(a: Int, b: Int): Int {
    return a + b
}


// FILE: b.kt
package foo

// CHECK_NOT_CALLED: sum

fun box(): String {
    val sum3 = sum(1, 2)
    assertEquals(3, sum3)

    return "OK"
}