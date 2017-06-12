// EXPECTED_REACHABLE_NODES: 492
package foo

// CHECK_NOT_CALLED: testInline
// CHECK_LABELS_COUNT: function=testNoinline name=loop count=2

inline fun testInline(): Int {
    var c = 0

    loop@ for (i in 1..9) {
        c++
        if (c == 2) break@loop
    }


    loop@ for (j in 1..9) {
        c++
        if (c == 4) break@loop
    }

    return c
}

fun testNoinline(): Int {
    return testInline()
}

fun box(): String {
    assertEquals(4, testNoinline())

    return "OK"
}