// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1116
package foo

fun box(): String {
    assertEquals(true, fizz(true) || buzz(true) && (fizz(false) || buzz(true)))
    assertEquals("fizz(true);", pullLog())

    return "OK"
}