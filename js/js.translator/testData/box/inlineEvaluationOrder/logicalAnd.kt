// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1116
package foo

fun box(): String {
    assertEquals(false, fizz(false) && buzz(false))
    assertEquals("fizz(false);", pullLog())

    assertEquals(false, fizz(true) && buzz(false))
    assertEquals("fizz(true);buzz(false);", pullLog())

    assertEquals(true, fizz(true) && buzz(true))
    assertEquals("fizz(true);buzz(true);", pullLog())

    return "OK"
}