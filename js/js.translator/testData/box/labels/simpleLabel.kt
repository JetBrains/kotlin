// EXPECTED_REACHABLE_NODES: 493
package foo

// CHECK_LABELS_COUNT: function=testBreak name=loop count=1
// CHECK_LABELS_COUNT: function=testContinue name=loop count=1

fun testBreak() {
    var i = 0

    loop@ for (j in 1..10) {
        if (j == 5) break@loop

        i = j
    }

    assertEquals(4, i, "break")
}

fun testContinue() {
    var sum = 0

    loop@ for (j in 1..5) {
        if (j % 2 != 0) continue@loop

        sum += j
    }

    assertEquals(6, sum, "continue")
}

fun box(): String {
    testBreak()
    testContinue()

    return "OK"
}