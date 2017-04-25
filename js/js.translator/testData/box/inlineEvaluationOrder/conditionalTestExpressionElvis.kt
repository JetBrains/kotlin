// EXPECTED_REACHABLE_NODES: 496
package foo

fun test(x: Boolean?): Boolean = buzz(x) ?: fizz(true)

fun box(): String {
    assertEquals(true, test(null))
    assertEquals("buzz(null);fizz(true);", pullLog())
    assertEquals(false, test(false))
    assertEquals("buzz(false);", pullLog())
    assertEquals(true, test(true))
    assertEquals("buzz(true);", pullLog())

    return "OK"
}