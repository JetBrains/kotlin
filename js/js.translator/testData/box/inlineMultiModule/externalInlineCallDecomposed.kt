// EXPECTED_REACHABLE_NODES: 500
// MODULE: lib
// FILE: lib.kt

package lib

var global = ""

inline fun baz(x: () -> Int) = A(1).bar(x())

class A(val y: Int) {
    fun bar(x: Int) = x + y
}


// MODULE: main(lib)
// FILE: main.kt

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