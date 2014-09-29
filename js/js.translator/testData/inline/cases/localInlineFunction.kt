package foo

// CHECK_CONTAINS_NO_CALLS: localWithCapture
// CHECK_CONTAINS_NO_CALLS: localWithoutCapture

inline fun repeat(times: Int, action: () -> Unit) {
    for (i in 1..times) {
        action()
    }
}

fun localWithoutCapture(a: Int, b: Int): Int {
    var sum = 0

    [inline] fun inc(x: Int): Int {
        return x + 1
    }

    repeat(a + b)  {
        sum = inc(sum)
    }

    return sum
}

fun localWithCapture(a: Int, b: Int): Int {
    var sum = 0

    [inline] fun inc() {
        sum++
    }

    repeat(a + b)  {
        inc()
    }

    return sum
}

fun box(): String {
    assertEquals(3, localWithoutCapture(1, 2), "localWithoutCapture")
    assertEquals(9, localWithoutCapture(4, 5), "localWithoutCapture")

    assertEquals(3, localWithCapture(1, 2), "localWithCapture")
    assertEquals(9, localWithCapture(4, 5), "localWithCapture")

    return "OK"
}