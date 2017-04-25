// EXPECTED_REACHABLE_NODES: 494
package foo

var global: String = ""

fun f(arg1: String, arg2: String, arg3: String): String {
    global += ":f:"
    return arg1 + arg2 + arg3
}

fun id(s: String): String {
    global += s
    return s
}

fun bar(s: String): String {
    return f(id(s), if (true) return ":bar:" else return "b", id(s))
}

fun box(): String {
    var b: Boolean

    var i = 0
    while (i++ < 5) {
        if (i == 2) f(id("A"), break, id("B"))
    }
    assertEquals(2, i, "break 1")
    assertEquals("A", global, "break 1")

    global = ""
    i = 0
    while (i++ < 5) {
        if (i == 2) {
            var x = f(id("A"), break, id("B"))
        }
    }
    assertEquals(2, i, "break 2")
    assertEquals("A", global, "break 2")

    global = ""
    i = 0
    while (i++ < 5) {
        if (i == 2) {
            var x = "B" + f(id("A"), break, id("B"))
        }
    }
    assertEquals(2, i, "break 3")
    assertEquals("A", global, "break 3")

    global = ""
    i = 0
    var n = 0
    while (i++ < 5) {
        if (i == 2) f(id("A"), continue, id("B"))
        n++
    }
    assertEquals(4, n, "continue 1")
    assertEquals("A", global, "continue 1")

    global = ""
    i = 0
    n = 0
    while (i++ < 5) {
        if (i == 2) {
            var x = f(id("A"), continue, id("B"))
        }
        n++
    }
    assertEquals(4, n, "continue 2")
    assertEquals("A", global, "continue 2")

    global = ""
    i = 0
    n = 0
    while (i++ < 5) {
        if (i == 2) {
            var x = "B" + f(id("A"), continue, id("B"))
        }
        n++
    }
    assertEquals(4, n, "continue 3")
    assertEquals("A", global, "continue 3")

    global = ""
    assertEquals(":bar:", bar("A"))
    assertEquals("A", global, "bar")

    return "OK"
}