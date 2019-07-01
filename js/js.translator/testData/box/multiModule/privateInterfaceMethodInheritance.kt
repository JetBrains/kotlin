// EXPECTED_REACHABLE_NODES: 1280
// IGNORE_BACKEND: JS_IR
// MODULE: lib
// FILE: lib.kt
package lib

interface A {
    private fun foo() = "OK"

    fun bar() = foo()
}

open class AProxy: A

// MODULE: main(lib)
// FILE: main.kt
package main

// KT-31007
import lib.AProxy

// Important bit: don't inherit A directly
class B : AProxy()

fun box(): String = B().bar()