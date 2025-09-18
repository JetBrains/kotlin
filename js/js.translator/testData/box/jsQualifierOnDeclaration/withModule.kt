// DONT_TARGET_EXACT_BACKEND: WASM_JS
// WASM_MUTE_REASON: MODULE_KIND

// MODULE: lib
// JS_MODULE_KIND: AMD
// FILE: lib.kt
@file:JsModule("libjs")
package ab

@JsQualifier("a.b")
external fun c(): String

// MODULE: main(lib)
// JS_MODULE_KIND: AMD
// FILE: main.kt
package main

fun box() = ab.c()
