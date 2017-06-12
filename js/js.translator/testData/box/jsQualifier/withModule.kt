// EXPECTED_REACHABLE_NODES: 489
// MODULE: lib
// FILE: lib.kt
// MODULE_KIND: AMD
@file:JsQualifier("a.b")
@file:JsModule("libjs")
package ab

external fun c(): String

// MODULE: main(lib)
// FILE: main.kt
// MODULE_KIND: AMD
package main

fun box() = ab.c()