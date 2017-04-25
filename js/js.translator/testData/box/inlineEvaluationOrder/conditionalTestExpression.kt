// EXPECTED_REACHABLE_NODES: 496
package foo

fun test(x: Boolean): Boolean =
        if (buzz(x)) buzz(true) else fizz(false)

fun box(): String {
    assertEquals(true, test(true))
    assertEquals("buzz(true);buzz(true);", pullLog())
    assertEquals(false, test(false))
    assertEquals("buzz(false);fizz(false);", pullLog())

    return "OK"
}