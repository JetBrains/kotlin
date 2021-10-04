// EXPECTED_REACHABLE_NODES: 1283
package foo

// CHECK_CONTAINS_NO_CALLS: myAdd

internal inline fun run(action: () -> Int): Int {
    return action()
}

internal fun myAdd(a: Int, b: Int): Int {
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
    assertEquals(3, myAdd(1, 2))

    return "OK"
}