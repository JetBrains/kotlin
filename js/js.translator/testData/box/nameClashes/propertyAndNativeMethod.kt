// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1285
// MODULE: lib
// FILE: lib.kt
package lib

external fun bar(): Int

val bar = 32

// FILE: lib.js
function bar() {
    return 23;
}

// MODULE: main(lib)
// FILE: main.kt
package main

import lib.*

fun box(): String {
    assertEquals(23, bar())
    assertEquals(32, bar)

    return "OK"
}