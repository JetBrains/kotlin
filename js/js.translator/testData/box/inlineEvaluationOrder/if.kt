// EXPECTED_REACHABLE_NODES: 496
package foo

fun test(x: Boolean, y: Boolean): Boolean {
    if (fizz(x) && buzz(y)) {
        return true
    }

    return false
}


fun box(): String {
    assertEquals(false, test(false, true))
    assertEquals("fizz(false);", pullLog())

    assertEquals(false, test(true, false))
    assertEquals("fizz(true);buzz(false);", pullLog())

    assertEquals(true, test(true, true))
    assertEquals("fizz(true);buzz(true);", pullLog())

    return "OK"
}