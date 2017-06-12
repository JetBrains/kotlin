// EXPECTED_REACHABLE_NODES: 497
// MODULE: lib
// FILE: lib.kt

package utils

inline
public fun <T, R> apply(x: T, crossinline fn: (T)->R): R {
    val result = object {
        val x = fn(x)
    }

    return result.x
}


// MODULE: main(lib)
// FILE: main.kt

import utils.*

internal fun test(x: Int): Int = apply(x) { it * 2 }

fun box(): String {
    assertEquals(6, test(3))

    return "OK"
}