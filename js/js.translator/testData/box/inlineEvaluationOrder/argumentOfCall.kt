// EXPECTED_REACHABLE_NODES: 497
package foo

fun sum(x: Int, y: Int): Int {
    log("sum($x, $y)")
    return x + y
}

fun box(): String {
    assertEquals(3, sum(fizz(1), buzz(2)))
    assertEquals("fizz(1);buzz(2);sum(1, 2);", pullLog())

    return "OK"
}