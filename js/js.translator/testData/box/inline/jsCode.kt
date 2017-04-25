// EXPECTED_REACHABLE_NODES: 492
package foo

// CHECK_CONTAINS_NO_CALLS: test

internal inline fun sum(x: Int, y: Int): Int = js("x + y")

internal fun test(x: Int, y: Int): Int = sum(sum(x, x), sum(y, y))

fun box(): String {
    assertEquals(4, test(1, 1))
    assertEquals(8, test(1, 3))

    return "OK"
}