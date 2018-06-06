// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1115
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