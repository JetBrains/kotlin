// IGNORE_BACKEND: JS
// SPLIT_PER_MODULE
// CALL_MAIN
// MODULE: lib
// FILE: lib.kt

package lib

var log: String = ""

val hack = ::main

fun main() {
    log += "fail"
}

// MODULE: main(lib)
// FILE: main.kt

package main

import lib.*

fun main() {
    log += "OK"
}

fun box(): String {
    return log
}
