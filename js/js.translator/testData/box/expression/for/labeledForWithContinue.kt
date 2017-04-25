// EXPECTED_REACHABLE_NODES: 493
// http://youtrack.jetbrains.com/issue/KT-5257
// JS: for with continue with label fails on runtime

package foo

var global: String = ""

fun up(s: String, value: Int): Int {
    global += s
    return value
}

fun box(): String {

    list@
    for (i in 0..up("A", 5)) {
        continue@list
    }
    assertEquals("A", global)

    global = ""
    list1@
    for (i in up("A", 0)..try { global += "B"; 5} finally {}) {
        continue@list1
    }
    assertEquals("AB", global)

    global = ""
    list2@
    for (i in try { up("A", 0) } finally {}..try { global += "B"; 5} finally {}) {
        continue@list2
    }
    assertEquals("AB", global)

    return "OK"
}