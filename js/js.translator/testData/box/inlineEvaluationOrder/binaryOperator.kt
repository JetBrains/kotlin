// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1116
package foo

fun box(): String {
    assertEquals(3, fizz(1) + buzz(2))
    assertEquals("fizz(1);buzz(2);", pullLog())

    return "OK"
}