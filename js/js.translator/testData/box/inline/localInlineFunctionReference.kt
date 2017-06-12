// EXPECTED_REACHABLE_NODES: 493
package foo

fun multiplyBy(a: Int): (Int) -> Int {
    inline fun multiply(b: Int): Int = a * b

    return ::multiply
}

fun box(): String {
    assertEquals(6, multiplyBy(2)(3))

    return "OK"
}