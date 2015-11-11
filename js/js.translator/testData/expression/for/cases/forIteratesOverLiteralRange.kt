package foo

var global: String = ""

fun up(s: String, value: Int): Int {
    global += s
    return value
}

fun box(): String {

    // http://youtrack.jetbrains.com/issue/KT-5262
    // JS: wrong code for `for` over range when start greater end
    var n: Int = 0
    for(i in 3..0)
        n++
    assertEquals(0, n)

    for(i in 0..3)
        n++
    assertEquals(4, n)

    // Evaluation order
    for (i in up("A", 0)..up("B", 5)) {
    }
    assertEquals("AB", global)

    global = ""
    for (i in try { up("A", 0) } finally {} ..up("B", 5)) {
    }
    assertEquals("AB", global)

    global = ""
    for (i in up("A", 0).. try { up("B", 5)} finally {}) {
    }
    assertEquals("AB", global)

    global = ""
    for (i in try { up("A", 0) } finally {}.. try { up("B", 5)} finally {}) {
    }
    assertEquals("AB", global)

    var sLong = 0L
    for(i in 0L..10L)
        sLong += i
    assertEquals(55L, sLong)


    return "OK"
}