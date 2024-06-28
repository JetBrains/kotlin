// EXPECTED_REACHABLE_NODES: 1282

// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: MODULE_KIND

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
