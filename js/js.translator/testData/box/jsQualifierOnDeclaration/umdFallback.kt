// DONT_TARGET_EXACT_BACKEND: WASM_JS
// WASM_MUTE_REASON: MODULE_KIND

// NO_JS_MODULE_SYSTEM
// MODULE: lib
// JS_MODULE_KIND: UMD
// FILE: lib.kt

@JsQualifier("foo")
external fun bar(): String

// MODULE: main(lib)
// JS_MODULE_KIND: UMD
// FILE: main.kt
fun box() = bar()
