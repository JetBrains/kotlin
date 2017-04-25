// EXPECTED_REACHABLE_NODES: 495
package foo

fun box(): String {
    assertEquals(10, (fizz(1) + buzz(2)) + (fizz(3) + buzz(4)))
    assertEquals("fizz(1);buzz(2);fizz(3);buzz(4);", pullLog())

    return "OK"
}