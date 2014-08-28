package foo

fun bar(b: Boolean): String {
    if (b || return "A")
        return "B"
    else
        return "C"
}

fun testBreak(b: Boolean, expected: Int) {
    var i = 0
    while (i++ < 5) {
        if (i == 2) b || break
    }
    assertEquals(expected, i, "break 1")

    i = 0
    while (i++ < 5) {
        if (i == 2) {
            var x = b || break
        }
    }
    assertEquals(expected, i, "break 2")
}

fun testContinue(b: Boolean, expected: Int) {
    var i = 0
    var n = 0
    while (i++ < 5) {
        if (i == 2) b || continue
        n++
    }
    assertEquals(expected, n, "continue 1")

    i = 0
    n = 0
    while (i++ < 5) {
        if (i == 2) {
            var x = b || continue
        }
        n++
    }
    assertEquals(expected, n, "continue 2")
}

fun box(): String {

    testBreak(true, 6)
    testBreak(false, 2)

    testContinue(true, 5)
    testContinue(false, 4)

    assertEquals("B", bar(true))
    assertEquals("A", bar(false))

    return "OK"
}