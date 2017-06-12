// EXPECTED_REACHABLE_NODES: 492
package foo

var global: String = ""

fun box(): String {
    var i:Int = 0

    i = 0
    global = ""
    do i++ while(try { global += "A"; i } finally {}  < 3)
    assertEquals("AAA", global)
    assertEquals(3, i)

    i = 0
    global = ""
    do {
        if (i==3) break
        i++
    } while(try { global += "A"; i } finally {} < 10)
    assertEquals("AAA", global)
    assertEquals(3, i)

    i = 0
    global = ""
    do
        try { global += "B"; i++} finally {}
    while( try { global += "A"; i } finally{} < 3)
    assertEquals("BABABA", global)
    assertEquals(3, i)

    return "OK"
}