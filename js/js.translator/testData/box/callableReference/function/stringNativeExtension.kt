// EXPECTED_REACHABLE_NODES: 1251
package foo

fun box(): String {
    var s = "abc"
    assertEquals("ABC", (String::toUpperCase)(s))

    return "OK"
}
