package foo

import lib.*

fun qqq(): Int {
    global += "qqq;"
    return 23
}

fun box(): String {
    assertEquals(24, baz {
        global += "before;"
        val result = qqq()
        global += "after;"
        result
    })
    assertEquals("before;qqq;after;", global)

    return "OK"
}