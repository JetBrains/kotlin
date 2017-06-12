// EXPECTED_REACHABLE_NODES: 495
package foo

private inline fun bar(predicate: (Char) -> Boolean): Int {
    var i = -1
    val str = "abc "
    do {
        i++
        if (i == 1) continue
        log(i.toString())
    } while (predicate(str[i]) && i < 3)
    return i
}

private fun test(c: Char): Int {
    return bar {
        log(it.toString())
        it != c
    }
}

fun box(): String {
    assertEquals(0, test('a'))
    assertEquals("0;a;", pullLog())

    assertEquals(1, test('b'))
    assertEquals("0;a;b;", pullLog())

    assertEquals(2, test('c'))
    assertEquals("0;a;b;2;c;", pullLog())

    assertEquals(3, test('*'))
    assertEquals("0;a;b;2;c;3; ;", pullLog())

    return "OK"
}