// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1286
package foo

fun box(): String {
    assertEquals(2, fizz(arrayOf(1, 2))[buzz(1)])
    assertEquals("fizz(1,2);buzz(1);", pullLog())

    return "OK"
}