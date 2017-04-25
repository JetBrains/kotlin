// EXPECTED_REACHABLE_NODES: 491
// MODULE: lib
// FILE: lib.kt
package lib

object O

// MODULE: main(lib)
// FILE: main.kt
package main

import lib.*

fun box(): String {
    var o: Any = O
    if (o !is O) return "fail1"
    if (!(o is O)) return "fail2"

    return "OK"
}