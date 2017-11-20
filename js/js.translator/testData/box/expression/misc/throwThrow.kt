// EXPECTED_REACHABLE_NODES: 1117
package foo

fun test() {
    throw throw Exception("catch me")
}

fun box(): String {
    try {
        test()
        error("exception not thrown")
    }
    catch (e: Exception) {
        assertEquals("catch me", e.message)
    }
    return "OK"
}