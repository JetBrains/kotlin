// EXPECTED_REACHABLE_NODES: 500
// MODULE: lib
// FILE: lib.kt
package lib

object O {
    val result = "OK"

    inline fun foo(): String {
        val o = object {
            fun bar() = O
        }
        return fetch(o.bar())
    }
}

fun fetch(o: O) = o.result


// MODULE: main(lib)
// FILE: main.kt
package main

import lib.*

fun box() = O.foo()