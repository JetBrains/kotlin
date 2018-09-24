// EXPECTED_REACHABLE_NODES: 1282
package foo

fun box(): String {
    var s: String = ""

    s = ""
    for(i in 0..2)
        try { s += "A"} finally {}
    assertEquals("AAA", s)

    return "OK"
}