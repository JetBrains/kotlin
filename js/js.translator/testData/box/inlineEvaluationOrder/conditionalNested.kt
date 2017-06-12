// EXPECTED_REACHABLE_NODES: 496
package foo

fun test(x: Boolean, y: Boolean): Int =
        if (fizz(x))
            if (fizz(y)) buzz(1) else buzz(2)
        else
            if (fizz(y)) buzz(3) else buzz(4)

fun box(): String {
    assertEquals(1, test(true, true))
    assertEquals("fizz(true);fizz(true);buzz(1);", pullLog())

    assertEquals(2, test(true, false))
    assertEquals("fizz(true);fizz(false);buzz(2);", pullLog())

    assertEquals(3, test(false, true))
    assertEquals("fizz(false);fizz(true);buzz(3);", pullLog())

    assertEquals(4, test(false, false))
    assertEquals("fizz(false);fizz(false);buzz(4);", pullLog())

    return "OK"
}