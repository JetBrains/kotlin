// EXPECTED_REACHABLE_NODES: 493
package foo

// CHECK_CONTAINS_NO_CALLS: doNothingInt
// CHECK_CONTAINS_NO_CALLS: doNothingStr

internal inline fun <T> doNothing(a: T): T {
    return a
}

internal fun doNothingInt(a: Int): Int {
    return doNothing(a)
}

internal fun doNothingStr(a: String): String {
    return doNothing(a)
}

fun box(): String {
    assertEquals(1, doNothingInt(1))
    assertEquals(2, doNothingInt(2))
    assertEquals("ab", doNothingStr("ab"))
    assertEquals("abc", doNothingStr("abc"))

    return "OK"
}