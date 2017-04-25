// EXPECTED_REACHABLE_NODES: 492
package foo

// CHECK_CONTAINS_NO_CALLS: test
// CHECK_VARS_COUNT: function=test count=2

internal inline fun sum(x: Int, y: Int): Int {
    if (x == 0 || y == 0) return 0

    return x + y
}

internal fun test(x: Int, y: Int): Int {
    val sum = sum(x, y)
    return sum
}

fun box(): String {
    assertEquals(3, test(1, 2))

    return "OK"
}