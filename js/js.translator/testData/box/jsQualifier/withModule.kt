// DONT_TARGET_EXACT_BACKEND: JS

// TODO: Support JS module qualifiers
// IGNORE_BACKEND: JS_IR

// MODULE: lib
// FILE: lib.kt
@file:JsQualifier("a.b")
@file:JsModule("./withModule.mjs")
package ab

external fun c(): String

// MODULE: main(lib)
// FILE: main.kt
package main

fun box() = ab.c()