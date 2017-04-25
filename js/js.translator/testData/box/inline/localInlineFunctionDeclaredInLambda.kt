// EXPECTED_REACHABLE_NODES: 493
package foo

// CHECK_CONTAINS_NO_CALLS: localWithCapture
// CHECK_CONTAINS_NO_CALLS: localWithoutCapture

internal inline fun repeatAction(times: Int, action: () -> Unit) {
    for (i in 1..times) {
        action()
    }
}

internal fun localWithoutCapture(a: Int, b: Int): Int {
    var mult = 0

    repeatAction(a) {
        inline fun inc(x: Int): Int {
            return x + 1
        }

        repeatAction(b) {
            mult = inc(mult)
        }
    }

    return mult
}

internal fun localWithCapture(a: Int, b: Int): Int {
    var mult = 0

    repeatAction(a) {
        inline fun inc() {
            mult++
        }

        repeatAction(b) {
            inc()
        }
    }

    return mult
}

fun box(): String {
    assertEquals(2, localWithoutCapture(1, 2), "localWithoutCapture")
    assertEquals(20, localWithoutCapture(4, 5), "localWithoutCapture")

    assertEquals(2, localWithCapture(1, 2), "localWithCapture")
    assertEquals(20, localWithCapture(4, 5), "localWithCapture")

    return "OK"
}