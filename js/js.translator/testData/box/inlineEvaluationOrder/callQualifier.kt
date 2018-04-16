// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1118
package foo

fun multiplyFun(): (Int, Int)->Int {
    log("multiplyFun()")
    return { x, y -> x * y }
}

fun box(): String {
    assertEquals(6, multiplyFun()(fizz(2), buzz(3)))
    assertEquals("multiplyFun();fizz(2);buzz(3);", pullLog())

    return "OK"
}