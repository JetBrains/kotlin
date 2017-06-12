// EXPECTED_REACHABLE_NODES: 491
package foo

fun box(): String {
    var s: String = ""

    var i:Int = 0
    while(i<2)
        try { s += "A"; i++} finally {}
    assertEquals("AA", s)

    s = ""
    i = 0
    do
        try { s += "A"; i++} finally {}
    while(i<2)
    assertEquals("AA", s)

    return "OK"
}