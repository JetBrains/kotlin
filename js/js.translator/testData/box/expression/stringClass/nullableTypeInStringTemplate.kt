// EXPECTED_REACHABLE_NODES: 491
// KT-2901 nullable type in string template
package foo

fun box(): String {
    var a: Int? = null

    assertEquals("a: null", "a: ${a}")

    a = 10
    assertEquals("a: 10", "a: ${a}")

    var s: String? = null
    assertEquals("s: null", "s: $s")

    s = "test"
    assertEquals("s: test", "s: $s")

    return "OK"
}