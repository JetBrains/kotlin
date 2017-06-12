// EXPECTED_REACHABLE_NODES: 491
package foo

// CHECK_NOT_CALLED: sum

inline fun sum(a: Int, b: Int): Int {
    return a + b
}

fun box(): String {
    val sum3 = sum(1, 2)
    assertEquals(3, sum3)

    val sum6 = sum(sum3, 3)
    assertEquals(6, sum6)

    return "OK"
}