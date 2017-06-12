// EXPECTED_REACHABLE_NODES: 493
package foo

fun callWithArgs(sumFunc: (Int, Int) -> Int, a: Int, b: Int): Int {
    return sumFunc(a, b)
}

fun box(): String {
    val kotlinSum: (Int, Int) -> Int = { a, b -> a + b}
    val jsSum: (Int, Int) -> Int = js("function (a, b) { return a + b; }")
    assertEquals(callWithArgs(kotlinSum, 1, 2), callWithArgs(jsSum, 1, 2))

    return "OK"
}