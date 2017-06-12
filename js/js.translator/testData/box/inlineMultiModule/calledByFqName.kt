// EXPECTED_REACHABLE_NODES: 494
// MODULE: lib
// FILE: lib.kt
package utils

inline
public fun sum(x: Int, y: Int): Int =
        x + y


// MODULE: main(lib)
// FILE: main.kt
// CHECK_CONTAINS_NO_CALLS: test

internal fun test(x: Int, y: Int): Int = utils.sum(x, y)

fun box(): String {
    assertEquals(3, test(1, 2))
    assertEquals(5, test(2, 3))

    return "OK"
}