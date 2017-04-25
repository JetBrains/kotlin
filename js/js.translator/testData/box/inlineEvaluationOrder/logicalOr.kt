// EXPECTED_REACHABLE_NODES: 495
package foo

fun box(): String {
    assertEquals(true, fizz(true) || buzz(false))
    assertEquals("fizz(true);", pullLog())

    assertEquals(true, fizz(false) || buzz(true))
    assertEquals("fizz(false);buzz(true);", pullLog())

    assertEquals(false, fizz(false) || buzz(false))
    assertEquals("fizz(false);buzz(false);", pullLog())

    return "OK"
}