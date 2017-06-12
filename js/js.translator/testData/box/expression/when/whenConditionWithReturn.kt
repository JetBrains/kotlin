// EXPECTED_REACHABLE_NODES: 492
package foo

fun test(): Int {
    return when {
        (return 23) is Int -> 24
        else -> 25
    }
}

fun box(): String {
    assertEquals(23, test())
    return "OK"
}