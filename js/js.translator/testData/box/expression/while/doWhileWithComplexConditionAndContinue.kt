// EXPECTED_REACHABLE_NODES: 492
package foo

var global: String = ""

fun box(): String {

    var i = 0

    i = 0
    global = ""
    do {
        if (i<3) {i++; continue}
        break
    } while(try { global += "A"; i } finally {} < 10)
    assertEquals("AAA", global)
    assertEquals(3, i)

    i = 0
    global = ""
    labelA@ do {
        if (i<3) {i++; continue@labelA}
        break
    } while(try { global += "A"; i } finally {} < 10)
    assertEquals("AAA", global)
    assertEquals(3, i)

    i = 0
    var j = 0
    global = ""
    outer@ do {
        j = 0
        while( try {global += "B"; j++ } finally {} < 2) {
            if (j==1) continue@outer
        }
    } while(try { global += "A"; i++ } finally {}  < 3)
    assertEquals("BABABABA", global)
    assertEquals(4, i)
    assertEquals(1, j)

    return "OK"
}