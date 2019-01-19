// KJS_WITH_FULL_RUNTIME
// EXPECTED_REACHABLE_NODES: 1282
package foo

fun box(): String {
    var s = "abc"
    assertEquals("ABC", (String::toUpperCase)(s))

    return "OK"
}
