// EXPECTED_REACHABLE_NODES: 492
package foo

var global: String = ""

fun box(): String {
    var i:Int = 0

    i = 0
    global = ""
    while(try { global += "A"; i } finally {}  < 3)
        i++
    assertEquals("AAAA", global)
    assertEquals(3, i)

    i = 0
    global = ""
    while(try { global += "A"; i } finally {} < 10) {
        if (i==3) break
        i++
    }
    assertEquals("AAAA", global)
    assertEquals(3, i)

    i = 0
    global = ""
    while( try { global += "A"; i } finally{} < 3)
        try { global += "B"; i++} finally {}
    assertEquals("ABABABA", global)

    return "OK"
}