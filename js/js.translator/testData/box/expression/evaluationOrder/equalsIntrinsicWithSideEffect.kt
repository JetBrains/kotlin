// EXPECTED_REACHABLE_NODES: 493
package foo

var global: String = ""

fun bar(s: String): String {
    global += s
    return s
}

fun box(): String {

    global = ""
    assertEquals(false, bar("A") == bar("B"))
    assertEquals("AB", global)

    global = ""
    assertEquals(true, bar("A") != bar("B"))
    assertEquals("AB", global)

    global = ""
    assertEquals(false, bar("A") == try { bar("B") } finally {})
    assertEquals("AB", global)

    global = ""
    assertEquals(true, bar("A") != try { bar("B") } finally {})
    assertEquals("AB", global)

    global = ""
    assertEquals(false, try { bar("A") } finally {}  == bar("B"))
    assertEquals("AB", global)

    global = ""
    assertEquals(true, try { bar("A") } finally {} != bar("B"))
    assertEquals("AB", global)

    global = ""
    assertEquals(false, try { bar("A") } finally {}  == try { bar("B") } finally {})
    assertEquals("AB", global)

    global = ""
    assertEquals(true, try { bar("A") } finally {} != try { bar("B") } finally {})
    assertEquals("AB", global)

    return "OK"
}