// EXPECTED_REACHABLE_NODES: 496
// MODULE: lib
// FILE: lib.kt

package utils

inline
public fun <T, R> apply(x: T, fn: T.()->R): R =
        x.fn()


// MODULE: main(lib)
// FILE: main.kt

import utils.*

// CHECK_CONTAINS_NO_CALLS: test except=imul

internal class A(val n: Int)

internal fun test(a: A, m: Int): Int = apply(a) { n * m }

fun box(): String {
    assertEquals(6, test(A(2), 3))

    return "OK"
}