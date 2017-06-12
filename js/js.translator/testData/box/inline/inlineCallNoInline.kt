// EXPECTED_REACHABLE_NODES: 494
package foo

// CHECK_NOT_CALLED: sumEvenInRange

fun sum(a: Int, b: Int): Int {
    return a + b
}

fun even(a: Int): Boolean {
    return a % 2 == 0
}

inline fun sumEvenInRange(a: Int, b: Int): Int {
    var c = 0

    for (i in a..b) {
        if (even(i)) {
            c = sum(c, i)
        }
    }

    return c
}

fun box(): String {
    val sum6 = sumEvenInRange(1, 5)
    assertEquals(6, sum6)

    val sum12 = sumEvenInRange(0, 7)
    assertEquals(12, sum12)

    val sum20 = sumEvenInRange(0, 9)
    assertEquals(20, sum20)

    return "OK"
}