// EXPECTED_REACHABLE_NODES: 496
// MODULE: lib
// FILE: lib.kt

package utils

public var LOG: String = ""

inline
public fun log(s: String): String {
    LOG += s
    return LOG
}


// MODULE: main(lib)
// FILE: main.kt

import utils.*

// CHECK_CONTAINS_NO_CALLS: test

internal fun test(s: String): String = log(s + ";")

fun box(): String {
    assertEquals("a;", test("a"))
    assertEquals("a;b;", test("b"))

    return "OK"
}