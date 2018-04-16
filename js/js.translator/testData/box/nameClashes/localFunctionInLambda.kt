// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1115
// MODULE: lib
// FILE: lib.kt
package lib

fun String.bar() = this

// MODULE: main(lib)
// FILE: main.kt
package foo

import lib.*

object O

fun box(): String = {
    fun O.bar() = "O"
    O.bar() + "K".bar()
}()