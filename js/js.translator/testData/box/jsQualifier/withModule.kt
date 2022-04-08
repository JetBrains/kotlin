// IGNORE_FIR
// EXPECTED_REACHABLE_NODES: 1282
// MODULE: lib
// MODULE_KIND: AMD
// FILE: lib.kt
@file:JsQualifier("a.b")
@file:JsModule("libjs")
package ab

external fun c(): String

// MODULE: main(lib)
// MODULE_KIND: AMD
// FILE: main.kt
package main

fun box() = ab.c()