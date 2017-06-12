// EXPECTED_REACHABLE_NODES: 496
package foo

class A

fun bar(a: A?): String {
    if ((a ?: return "A") != null)
        return "B"
    else
        return "C"
}

fun testBreak(a: A?, expected: Int) {
    var i = 0
    while(i++<5) {
        if (i==2) a ?: break
    }
    assertEquals(expected, i, "break 1")

    i = 0
    while(i++<5) {
        if (i==2) {
            var x = a ?: break
        }
    }
    assertEquals(expected, i, "break 2")
}

fun testContinue(a: A?, expected: Int) {
    var i = 0
    var n = 0
    while(i++<5) {
        if (i==2) a ?: continue
        n++
    }
    assertEquals(expected, n)

    i = 0
    n = 0
    while(i++<5) {
        if (i==2)  {
            var x = a ?: continue
        }
        n++
    }
    assertEquals(expected, n)
}

fun box(): String {

    testBreak(null, 2)
    testBreak(A(), 6)

    var i = 0
    while(i++<5) {
        if (i==2) break ?: null
    }
    assertEquals(2, i, "break ?: null")

    testContinue(null, 4)
    testContinue(A(), 5)

    i = 0
    var n = 0
    while(i++<5) {
        if (i==2) continue ?: null
        n++
    }
    assertEquals(4, n, "continue ?: null")

    assertEquals("A", bar(null))
    assertEquals("B", bar(A()))

    return "OK"
}