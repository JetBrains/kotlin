// EXPECTED_REACHABLE_NODES: 494
// MODULE: lib
// FILE: lib.kt
package lib

object O {
    operator fun invoke() = "OK"
}

inline fun callO() = O()

// MODULE: main(lib)
// FILE: main.kt
package main

import lib.*

fun box(): String {
    val a = O()
    if (a != "OK") return "fail: simple: $a"

    val b = callO()
    if (b != "OK") return "fail: inline: $a"

    return "OK"
}