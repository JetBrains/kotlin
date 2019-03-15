// EXPECTED_REACHABLE_NODES: 1283
package foo

fun box(): String {

    var s: String = ""

    try {
        throw Exception()
    } catch (e: Throwable) {
        s = "Throwable"
    }
    assertEquals("Throwable", s)

    return "OK"
}