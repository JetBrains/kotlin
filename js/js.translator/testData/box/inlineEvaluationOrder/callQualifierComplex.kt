// EXPECTED_REACHABLE_NODES: 498
package foo

// CHECK_NOT_CALLED: multiplyFunInline

fun multiplyFun(): (Int, Int)->Int {
    log("multiplyFun()")
    return { x, y -> x * y }
}

inline
fun multiplyFunInline(): (Int, Int)->Int {
    log("multiplyFunInline()")
    return { x, y -> x * y }
}

fun box(): String {
    assertEquals(6, arrayOf(multiplyFun(), multiplyFunInline())[0](fizz(2), fizz(3)))
    assertEquals("multiplyFun();multiplyFunInline();fizz(2);fizz(3);", pullLog())

    return "OK"
}