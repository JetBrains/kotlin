// EXPECTED_REACHABLE_NODES: 500
package foo

fun box(): String {
    assertArrayEquals(arrayOf(arrayOf(1, 2), arrayOf(3, 4)), arrayOf(arrayOf(fizz(1), buzz(2)), arrayOf(fizz(3), buzz(4))))
    assertEquals("fizz(1);buzz(2);fizz(3);buzz(4);", pullLog())

    return "OK"
}