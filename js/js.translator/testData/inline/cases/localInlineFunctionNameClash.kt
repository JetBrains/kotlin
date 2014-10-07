package foo

// CHECK_CONTAINS_NO_CALLS: add

inline fun run(action: () -> Int): Int {
    return action()
}

fun add(a: Int, b: Int): Int {
    var sum = a + b

    [inline] fun getSum(): Int {
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