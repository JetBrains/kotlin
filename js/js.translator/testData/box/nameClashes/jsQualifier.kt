// MODULE: lib
// FILE: lib.kt
@file:JsQualifier("chopchop.foo")

external fun ok(): String

// MODULE: lib2
// FILE: lib2.kt

@JsExport
fun chopchop() {}

// MODULE: main(lib, lib2)
// FILE: main.kt

fun box() = ok()
