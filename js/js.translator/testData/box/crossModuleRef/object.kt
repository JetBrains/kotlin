// EXPECTED_REACHABLE_NODES: 496
// MODULE: lib
// FILE: lib.kt
package lib

var log = ""

object O {
    init {
        log += "O.init;"
    }

    fun result() = "OK"
}

fun getResult(): String {
    log += "before;"
    val result = O.result()
    log += "after;"
    return result
}

// MODULE: main(lib)
// FILE: main.kt
package main

import lib.*

fun box(): String {
    val result = getResult()
    if (result != "OK") return "fail: unexpected result: $result"

    if (log != "before;O.init;after;") return "fail: wrong evaluation order: $log"

    return "OK"
}