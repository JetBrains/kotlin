// EXPECTED_REACHABLE_NODES: 1110
// MODULE: lib
// FILE: lib.kt
@file:JsQualifier("a.b")
package ab

external fun c(): String

external val d: String

// MODULE: main(lib)
// FILE: main.kt

package main

fun box() = ab.c() + ab.d