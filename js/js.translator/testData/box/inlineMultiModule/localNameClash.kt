// EXPECTED_REACHABLE_NODES: 494
// MODULE: lib
// FILE: lib.kt

package utils

inline
public fun <T, R> apply(x: T, fn: (T)->R): R {
    val y = fn(x)
    return y
}


// MODULE: main(lib)
// FILE: main.kt

import utils.*

// CHECK_CONTAINS_NO_CALLS: test except=imul

internal fun test(x: Int, y: Int): Int = apply(x) { it + 1 } * y

fun box(): String {
    assertEquals(6, test(1, 3))

    return "OK"
}