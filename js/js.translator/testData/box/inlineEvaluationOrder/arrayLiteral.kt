// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1289
package foo

fun box(): String {
    assertArrayEquals(arrayOf(1, 2), arrayOf(fizz(1), buzz(2)))
    assertEquals("fizz(1);buzz(2);", pullLog())

    return "OK"
}