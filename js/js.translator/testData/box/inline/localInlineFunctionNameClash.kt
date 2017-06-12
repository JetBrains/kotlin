// EXPECTED_REACHABLE_NODES: 493
package foo

// CHECK_CONTAINS_NO_CALLS: add

internal inline fun run(action: () -> Int): Int {
    return action()
}

internal fun add(a: Int, b: Int): Int {
    var sum = a + b

    inline fun getSum(): Int {
        return sum
    }

    return run {
        var sum = 0

        run {
            sum = -1
            getSum()
        }
    }
}

fun box(): String {
    assertEquals(3, add(1, 2))

    return "OK"
}