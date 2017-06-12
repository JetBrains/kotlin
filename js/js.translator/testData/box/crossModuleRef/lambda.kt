// EXPECTED_REACHABLE_NODES: 489
// MODULE: lib
// FILE: lib.kt
package lib

fun bar(f: () -> String) = f()

inline fun foo(): String {
    return bar { "OK" }
}

// MODULE: main(lib)
// FILE: main.kt
package main

import lib.*

fun box() = foo()