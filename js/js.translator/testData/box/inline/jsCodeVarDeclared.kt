// EXPECTED_REACHABLE_NODES: 492
package foo

// CHECK_CONTAINS_NO_CALLS: test

internal inline fun sum(x: Int, y: Int): Int = js("var a = x; a + y")

internal fun test(x: Int, y: Int): Int {
    val xx = sum(x, x)
    js("var a = 0;")
    val yy = sum(y, y)

    return sum(xx, yy)
}

fun box(): String {
    assertEquals(4, test(1, 1))
    assertEquals(8, test(1, 3))

    return "OK"
}