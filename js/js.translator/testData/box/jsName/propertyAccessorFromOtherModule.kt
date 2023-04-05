// EXPECTED_REACHABLE_NODES: 1281
// MODULE: lib
// FILE: lib.kt
package lib

val foo: Int
    @JsName("getBar") get() = 23

// MODULE: main(lib)
// FILE: main.kt
package main

import lib.*

fun box(): String {
    if (foo != 23) return "fail: $foo"
    return "OK"
}
