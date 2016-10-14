// MODULE: lib
// FILE: lib.kt

package utils

inline
public fun <T, R> apply(x: T, fn: (T)->R): R =
        fn(x)


// MODULE: main(lib)
// FILE: main.kt

import utils.*

// CHECK_CONTAINS_NO_CALLS: test_0

internal fun test(x: Int): Int = apply(x) { it * 2 }

fun box(): String {
    assertEquals(6, test(3))

    return "OK"
}