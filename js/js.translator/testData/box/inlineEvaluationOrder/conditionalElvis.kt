// EXPECTED_REACHABLE_NODES: 496
package foo

fun test(x: Boolean?): Boolean = fizz(x) ?: buzz(true)

fun box(): String {
    assertEquals(true, test(null))
    assertEquals("fizz(null);buzz(true);", pullLog())
    assertEquals(false, test(false))
    assertEquals("fizz(false);", pullLog())
    assertEquals(true, test(true))
    assertEquals("fizz(true);", pullLog())

    return "OK"
}