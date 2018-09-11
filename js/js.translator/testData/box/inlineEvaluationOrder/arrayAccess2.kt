// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1286
package foo

fun box(): String {
    assertEquals(2, arrayOf(1, 2)[fizz(0) + buzz(1)])
    assertEquals("fizz(0);buzz(1);", pullLog())

    return "OK"
}