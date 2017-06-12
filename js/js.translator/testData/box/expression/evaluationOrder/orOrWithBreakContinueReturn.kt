// EXPECTED_REACHABLE_NODES: 496
package foo

var global: String = ""

fun id(s: String, b: Boolean): Boolean {
    global += s
    return b
}

fun bar(b: Boolean): String {
    if (id("A",b) || return "A")
        return "B"
    else
        return "C"
}

fun testBreak(b: Boolean, expected: Int) {
    global = ""
    var i = 0
    while (i++ < 5) {
        if (i == 2) id("A", b) || break
    }
    assertEquals(expected, i, "break 1")
    assertEquals("A", global, "break 1")

    global = ""
    i = 0
    while (i++ < 5) {
        if (i == 2) {
            var x = id("A", b) || break
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
        if (i == 2) id("A", b) || continue
        n++
    }
    assertEquals(expected, n, "continue 1")
    assertEquals("A", global, "continue 1")

    global = ""
    i = 0
    n = 0
    while (i++ < 5) {
        if (i == 2) {
            var x = id("A", b) || continue
        }
        n++
    }
    assertEquals(expected, n, "continue 2")
    assertEquals("A", global, "continue 2")
}

fun box(): String {

    testBreak(true, 6)
    testBreak(false, 2)

    global = ""
    var i = 0
    while (i++ < 5) {
        if (i == 2) break || id("A", true)
    }
    assertEquals(2, i, "break || true")
    assertEquals("", global, "break || false")

    testContinue(true, 5)
    testContinue(false, 4)

    global = ""
    i = 0
    var n = 0
    while (i++ < 5) {
        if (i == 2) continue || id("A", true)
        n++
    }
    assertEquals(4, n, "continue || true")
    assertEquals("", global, "continue || true")

    assertEquals("B", bar(true))
    assertEquals("A", global, "bar")
    global = ""
    assertEquals("A", bar(false))
    assertEquals("A", global, "bar")

    return "OK"
}