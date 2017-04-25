// EXPECTED_REACHABLE_NODES: 487
package foo

// CHECK_NOT_CALLED: testBreak
// CHECK_NOT_CALLED: testContinue
// CHECK_LABELS_COUNT: function=testBreakNoinline name=loop count=1
// CHECK_LABELS_COUNT: function=testContinueNoinline name=loop count=1

inline fun testBreak(): Int {
    var i = 0

    loop@ for (j in 1..10) {
        if (j == 5) break@loop

        i = j
    }

    return i
}

fun testBreakNoinline() {
    assertEquals(4, testBreak(), "break")
}

inline fun testContinue(): Int {
    var sum = 0

    loop@ for (j in 1..5) {
        if (j % 2 != 0) continue@loop

        sum += j
    }

    return sum
}

fun testContinueNoinline() {
    assertEquals(6, testContinue(), "continue")
}

fun box(): String {
    testContinue()

    return "OK"
}