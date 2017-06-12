// EXPECTED_REACHABLE_NODES: 492
package foo

// CHECK_CONTAINS_NO_CALLS: test

internal fun test(x: Int, y: Int): Int =
        with (x + x) {
            val xx = this

            with (y + y) {
                xx + this
            }
        }

fun box(): String {
    assertEquals(10, test(2, 3))
    assertEquals(18, test(4, 5))

    return "OK"
}