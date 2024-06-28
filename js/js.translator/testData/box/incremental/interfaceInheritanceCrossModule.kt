// NO_COMMON_FILES
// MODULE: lib
// FILE: lib.kt

package lib

interface A {
    fun foo() = 23
}

// MODULE: main(lib)
// FILE: main.kt
// RECOMPILE

package main

import lib.A

class B : A {
    fun bar() = foo() + 1
}

fun box(): String {
    val result = B().bar()
    if (result != 24) return "fail: $result"
    return "OK"
}
