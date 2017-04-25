// EXPECTED_REACHABLE_NODES: 496
package foo

var global: String = ""

fun id(s: String, b: Boolean): Boolean {
    global += s
    return b
}

fun bar(b: Boolean): String {
    if (id("A", b) && return "A")
        return "B"
    else
        return "C"
}

fun testBreak(b: Boolean, expected: Int) {
    global = ""
    var i = 0
    while (i++ < 5) {
        if (i == 2) id("A", b) && break
    }
    assertEquals(expected, i, "break 1")
    assertEquals("A", global, "break 1")

    global = ""
    i = 0
    while (i++ < 5) {
        if (i == 2) {
            var x = id("A", b) && break
        }
    }
    assertEquals(expected, i, "break 2")
    assertEquals("A", global, "break 2")
}

fun testContinue(b: Boolean, expected: Int) {
    global = ""
    var i = 0
    var n = 0
    while (i++ < 5) {
        if (i == 2) id("A", b) && continue
        n++
    }
    assertEquals(expected, n, "continue 1")
    assertEquals("A", global, "continue 1")

    global = ""
    i = 0
    n = 0
    while (i++ < 5) {
        if (i == 2) {
            var x = id("A", b) && continue
            assertEquals(x, b)
        }
        n++
    }
    assertEquals(expected, n, "continue 2")
    assertEquals("A", global, "continue 2")
}

fun box(): String {
    var b: Boolean

    testBreak(true, 2)
    testBreak(false, 6)

    global = ""
    var i = 0
    while (i++ < 5) {
        if (i == 2) break && id("A", false)
    }
    assertEquals(2, i, "break && false")
    assertEquals("", global, "break && false")

    testContinue(true, 4)
    testContinue(false, 5)

    global = ""
    i = 0
    var n = 0
    while (i++ < 5) {
        if (i == 2) continue && id("A", false)
        n++
    }
    assertEquals(4, n, "continue && false")
    assertEquals("", global, "continue && false")

    assertEquals("A", bar(true), "bar")
    assertEquals("A", global, "bar")

    global = ""
    assertEquals("C", bar(false))
    assertEquals("A", global, "bar")

    return "OK"
}