package foo

// CHECK_NOT_CALLED: testLabelInline
// CHECK_LABELS_COUNT: function=testLabel name=loop count=1
// CHECK_LABELS_COUNT: function=testLabel name=loop_0 count=1
// CHECK_LABELS_COUNT: function=testLabel name=loop_1 count=1

inline fun testLabelInline(): Int {
    var a = 0

    loop@ for (i in 1..10) {
        if (i == 1) continue@loop

        a += i

        if (i == 2) break@loop
    }

    loop@ for (i in 1..10) {
        if (i == 1) continue@loop

        a += i

        if (i == 2) break@loop
    }

    return a
}

fun testLabel(): String {
    var a = 0

    loop@ for (i in 1..10) {
        if (i == 1) continue@loop

        a += testLabelInline()

        if (i == 2) break@loop
    }

    if (a != 4) return a.toString()

    return "OK"
}

fun box(): String {
    assertEquals("OK", testLabel())

    return "OK"
}