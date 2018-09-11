// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1284
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