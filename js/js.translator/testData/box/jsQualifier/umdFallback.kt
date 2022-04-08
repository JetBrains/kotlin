// IGNORE_FIR
// EXPECTED_REACHABLE_NODES: 1284
// NO_JS_MODULE_SYSTEM
// MODULE: lib
// MODULE_KIND: UMD
// FILE: lib.kt
@file:JsQualifier("foo")

external fun bar(): String

// MODULE: main(lib)
// MODULE_KIND: UMD
// FILE: main.kt
fun box() = bar()