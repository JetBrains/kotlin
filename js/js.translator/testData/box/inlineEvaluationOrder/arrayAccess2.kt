// EXPECTED_REACHABLE_NODES: 495
package foo

fun box(): String {
    assertEquals(2, arrayOf(1, 2)[fizz(0) + buzz(1)])
    assertEquals("fizz(0);buzz(1);", pullLog())

    return "OK"
}