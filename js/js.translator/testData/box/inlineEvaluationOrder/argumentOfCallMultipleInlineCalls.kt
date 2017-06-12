// EXPECTED_REACHABLE_NODES: 497
package foo

fun sum(a: Int, b: Int, c: Int, d: Int): Int {
    log("sum($a, $b, $c, $d)")
    return a + b + c + d
}

fun box(): String {
    assertEquals(10, sum(fizz(1), buzz(2), fizz(3), buzz(4)))
    assertEquals("fizz(1);buzz(2);fizz(3);buzz(4);sum(1, 2, 3, 4);", pullLog())

    return "OK"
}