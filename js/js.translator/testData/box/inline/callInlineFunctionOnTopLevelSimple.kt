// EXPECTED_REACHABLE_NODES: 495
package foo

// CHECK_NOT_CALLED: abs

inline fun abs(a: Int): Int {
    if (a < 0) {
        return a * -1
    } else {
        return a
    }
}

val r1 = abs(1)
val r2 = abs(-2)
val r3 = abs(3)
val r4 = abs(-4)

fun box(): String {
    assertEquals(1, r1)
    assertEquals(2, r2)
    assertEquals(3, r3)
    assertEquals(4, r4)

    return "OK"
}
