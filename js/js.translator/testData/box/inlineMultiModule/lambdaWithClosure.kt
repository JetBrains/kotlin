// EXPECTED_REACHABLE_NODES: 494
// MODULE: lib
// FILE: lib.kt

package utils

inline
public fun <T, R> apply(x: T, fn: (T)->R): R =
        fn(x)


// MODULE: main(lib)
// FILE: main.kt

import utils.*

// CHECK_CONTAINS_NO_CALLS: test

internal fun test(x: Int, y: Int): Int = apply(x) { it + y }

fun box(): String {
    assertEquals(3, test(1, 2))

    return "OK"
}