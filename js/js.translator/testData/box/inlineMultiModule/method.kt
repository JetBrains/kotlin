// EXPECTED_REACHABLE_NODES: 499
// MODULE: lib
// FILE: lib.kt

package utils

public class A(public val x: Int) {
    inline
    public fun plus(y: Int): Int = x + y
}


// MODULE: main(lib)
// FILE: main.kt

import utils.*

// CHECK_CONTAINS_NO_CALLS: test

internal fun test(a: A, y: Int): Int = a.plus(y)

fun box(): String {
    assertEquals(5, test(A(2), 3))

    return "OK"
}