// EXPECTED_REACHABLE_NODES: 1284
// IGNORE_BACKEND: JS_IR
// MODULE: lib
// FILE: lib.kt
// MODULE_KIND: UMD
// NO_JS_MODULE_SYSTEM
@file:JsQualifier("foo")

external fun bar(): String

// MODULE: main(lib)
// FILE: main.kt
// MODULE_KIND: UMD
// NO_JS_MODULE_SYSTEM
fun box() = bar()