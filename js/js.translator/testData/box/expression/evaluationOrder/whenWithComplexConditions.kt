// EXPECTED_REACHABLE_NODES: 493
package foo

var global: String = ""

fun id(s: String, value: Boolean): Boolean {
    global += s
    return value
}

fun box(): String {

    when {
      id("A", true) || id("B", true) -> 10
    }
    assertEquals("A", global)

    global = ""
    when {
        id("A", false) || id("B", true) || id("C", true) -> 10
    }
    assertEquals("AB", global)

    global = ""
    var b = true
    when {
        try { global += "A"; b } finally {} -> 10
        try { global += "B"; !b } finally {} -> 20
    }
    assertEquals("A", global)

    global = ""
    b = false
    when {
        try { global += "A"; b } finally {} -> 10
        try { global += "B"; !b } finally {} -> 20
    }
    assertEquals("AB", global)

    global = ""
    b = true
    when {
        b || try { global += "A"; !b } finally {} -> 10
    }
    assertEquals("", global)

    global = ""
    b = false
    when {
        b || try { global += "A"; !b } finally {} -> 10
    }
    assertEquals("A", global)

    global = ""
    when {
        false -> {
            global += "A"
        }
        try { global += "B"; false } finally {} -> {
            global += "D"
        }
        else -> {
            global += "C"
        }
    }
    assertEquals("BC", global)

    return "OK"
}