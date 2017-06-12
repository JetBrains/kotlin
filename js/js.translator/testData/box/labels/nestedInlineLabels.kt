// EXPECTED_REACHABLE_NODES: 494
package foo

var state = false

inline fun blockImpl(p: () -> Unit) {
    if (state) return

    p()
}

inline fun block(p: () -> Unit) {
    if (state) return

    blockImpl(p)
}

fun test(x: Int): Int {
    block outer@ {
        block inner@ {
            if (x < 10) return@inner
            if (x == 10) return@outer

            block innermost@ {
                if (x > 500) {
                    return 500500
                }
            }

            return x
        }

        if (x < 5) return@outer
        return x + 10
    }
    return x + 100
}

fun box(): String {
    assertEquals(16, test(6))
    assertEquals(104, test(4))
    assertEquals(110, test(110))
    assertEquals(11, test(11))
    assertEquals(500500, test(502))

    return "OK"
}