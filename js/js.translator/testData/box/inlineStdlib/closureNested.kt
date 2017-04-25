// EXPECTED_REACHABLE_NODES: 492
package foo

// CHECK_CONTAINS_NO_CALLS: test

internal fun test(a: Int, b: Int): Int {
    var res = 0

    with (a + b) {
        val t = this

        repeat(t) {
            res += t - b
        }
    }

    return res
}

fun box(): String {
    assertEquals(10, test(2, 3))
    assertEquals(15, test(3, 2))

    return "OK"
}