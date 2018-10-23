// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1282
package foo

fun box(): String {
    var s = "abc"
    assertEquals("ABC", (String::toUpperCase)(s))

    return "OK"
}
