// MODULE: lib
// FILE: lib.kt

package lib

internal fun foo() = 1

internal val bar = 2

// MODULE: main(lib)(lib)
// FILE: main.kt

package main

import lib.*

fun box(): String {
    if (foo() != 1) return "fail: ${foo()}"
    if (bar != 2) return "fail: ${bar}"
    return "OK"
}