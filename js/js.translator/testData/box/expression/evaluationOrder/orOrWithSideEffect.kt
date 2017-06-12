// EXPECTED_REACHABLE_NODES: 493
package foo

var global: String = ""

fun bar(s: String, value: Boolean): Boolean {
    global += s
    return value
}

fun box(): String {
    val a =  if (true || if(bar("A", false)) {2} else {3} == 0) true else false
    assertEquals("", global)

    true || if(bar("A", false)) {2} else {3} == 0
    assertEquals("", global)

    // Simple || Simple
    global = ""
    assertEquals(true, bar("A", true) || bar("B", true))
    assertEquals("A", global)

    global = ""
    assertEquals(true, bar("A", true) || bar("B", false))
    assertEquals("A", global)

    global = ""
    assertEquals(true, bar("A", false) || bar("B", true))
    assertEquals("AB", global)

    global = ""
    assertEquals(false, bar("A", false) || bar("B", false))
    assertEquals("AB", global)

    // Simple || Complex
    global = ""
    assertEquals(true, bar("A", true) || try { bar("B", true) } finally {})
    assertEquals("A", global)

    global = ""
    assertEquals(true, bar("A", true) || try { bar("B", false) } finally {})
    assertEquals("A", global)

    global = ""
    assertEquals(true, bar("A", false) || try { bar("B", true) } finally {})
    assertEquals("AB", global)

    global = ""
    assertEquals(false, bar("A", false) || try { bar("B", false) } finally {})
    assertEquals("AB", global)

    // Complex || Simple
    global = ""
    assertEquals(true, try { bar("A", true) } finally {} || bar("B", true))
    assertEquals("A", global)

    global = ""
    assertEquals(true, try { bar("A", true) } finally {} || bar("B", false))
    assertEquals("A", global)

    global = ""
    assertEquals(true, try { bar("A", false) } finally {} || bar("B", true))
    assertEquals("AB", global)

    global = ""
    assertEquals(false, try { bar("A", false) } finally {} || bar("B", false))
    assertEquals("AB", global)

    // Complex || Complex
    global = ""
    assertEquals(true, try { bar("A", true) } finally {} || try { bar("B", true) } finally {})
    assertEquals("A", global)

    global = ""
    assertEquals(true, try { bar("A", true) } finally {} || try { bar("B", false) } finally {})
    assertEquals("A", global)

    global = ""
    assertEquals(true, try { bar("A", false) } finally {} || try { bar("B", true) } finally {})
    assertEquals("AB", global)

    global = ""
    assertEquals(false, try { bar("A", false) } finally {} || try { bar("B", false) } finally {})
    assertEquals("AB", global)

    return "OK"
}