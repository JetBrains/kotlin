// EXPECTED_REACHABLE_NODES: 496
package foo

fun test(x: Boolean): Boolean =
        if (fizz(x)) buzz(true) else buzz(false)

fun box(): String {
    assertEquals(true, test(true))
    assertEquals("fizz(true);buzz(true);", pullLog())
    assertEquals(false, test(false))
    assertEquals("fizz(false);buzz(false);", pullLog())

    return "OK"
}