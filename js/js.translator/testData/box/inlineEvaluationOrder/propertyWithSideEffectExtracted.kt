// EXPECTED_REACHABLE_NODES: 495
// Looks similar to KT-7674
package foo

inline fun bar(): Int {
    log("bar")
    return 10
}

val x: Int
    get() {
        log("x")
        return 1
    }

fun box(): String {
    assertEquals(12, x + bar() + x)
    assertEquals("x;bar;x;", pullLog())

    return "OK"
}