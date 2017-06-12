// EXPECTED_REACHABLE_NODES: 493
// See KT-8005
package foo

private inline fun bar(predicate: (Char) -> Boolean): Int {
    var i = 0
    val str = "abc "
    while (predicate(str[i]) && i < 3) {
        i++
    }
    return i
}

private fun test(c: Char): Int {
    return bar { it != c }
}

fun box(): String {
    assertEquals(0, test('a'))
    assertEquals(1, test('b'))
    assertEquals(2, test('c'))
    assertEquals(3, test('*'))

    return "OK"
}