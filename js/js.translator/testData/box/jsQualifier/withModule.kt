// DONT_TARGET_EXACT_BACKEND: WASM_JS
// WASM_MUTE_REASON: MODULE_KIND

// MODULE: lib
// JS_MODULE_KIND: AMD
// FILE: lib.kt
@file:JsQualifier("a.b")
@file:JsModule("libjs")
package ab

external fun c(): String

// MODULE: main(lib)
// JS_MODULE_KIND: AMD
// FILE: main.kt
package main

fun box() = ab.c()
