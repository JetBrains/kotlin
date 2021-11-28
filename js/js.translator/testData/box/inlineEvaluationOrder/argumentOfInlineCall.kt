// EXPECTED_REACHABLE_NODES: 1286
package foo

// CHECK_NOT_CALLED_IN_SCOPE: function=max scope=box

inline fun max(a: Int, b: Int): Int {
    log("max($a, $b)")

    if (a > b) return a

    return b
}

fun box(): String {
    val test = max(fizz(1), max(fizz(2), buzz(3)))
    assertEquals(3, test)
    assertEquals("fizz(1);fizz(2);buzz(3);max(2, 3);max(1, 3);", pullLog())

    return "OK"
}