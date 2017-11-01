// EXPECTED_REACHABLE_NODES: 1255
package foo

fun box(): String {
    assertEquals(true, fizz(true) || buzz(true) && (fizz(false) || buzz(true)))
    assertEquals("fizz(true);", pullLog())

    return "OK"
}