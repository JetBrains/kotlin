// EXPECTED_REACHABLE_NODES: 493
package foo

var global: String = ""

fun bar(s: String, value: Boolean): Boolean {
    global += s
    return value
}

fun box(): String {

    // Simple && Simple
    global = ""
    assertEquals(true, bar("A", true) && bar("B", true))
    assertEquals("AB", global)

    global = ""
    assertEquals(false, bar("A", true) && bar("B", false))
    assertEquals("AB", global)

    global = ""
    assertEquals(false, bar("A", false) && bar("B", true))
    assertEquals("A", global)

    global = ""
    assertEquals(false, bar("A", false) && bar("B", false))
    assertEquals("A", global)

    // Simple && Complex
    global = ""
    assertEquals(true, bar("A", true) && try { bar("B", true) } finally {})
    assertEquals("AB", global)

    global = ""
    assertEquals(false, bar("A", true) && try { bar("B", false) } finally {})
    assertEquals("AB", global)

    global = ""
    assertEquals(false, bar("A", false) && try { bar("B", true) } finally {})
    assertEquals("A", global)

    global = ""
    assertEquals(false, bar("A", false) && try { bar("B", false) } finally {})
    assertEquals("A", global)

    // Complex && Simple
    global = ""
    assertEquals(true, try { bar("A", true) } finally {} && bar("B", true))
    assertEquals("AB", global)

    global = ""
    assertEquals(false, try { bar("A", true) } finally {} && bar("B", false))
    assertEquals("AB", global)

    global = ""
    assertEquals(false, try { bar("A", false) } finally {} && bar("B", true))
    assertEquals("A", global)

    global = ""
    assertEquals(false, try { bar("A", false) } finally {} && bar("B", false))
    assertEquals("A", global)

    // Complex && Complex
    global = ""
    assertEquals(true, try { bar("A", true) } finally {} && try { bar("B", true) } finally {})
    assertEquals("AB", global)

    global = ""
    assertEquals(false, try { bar("A", true) } finally {} && try { bar("B", false) } finally {})
    assertEquals("AB", global)

    global = ""
    assertEquals(false, try { bar("A", false) } finally {} && try { bar("B", true) } finally {})
    assertEquals("A", global)

    global = ""
    assertEquals(false, try { bar("A", false) } finally {} && try { bar("B", false) } finally {})
    assertEquals("A", global)

    return "OK"
}