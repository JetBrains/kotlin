// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1283

// MODULE: lib
// FILE: a.kt
// MODULE_KIND: AMD
@file:JsModule("foo")

external fun fooF(): String

// FILE: b.kt
@file:JsModule("bar")

external fun barF(): String

// FILE: c.kt
// RECOMPILE
fun dummyF() = "dummy"

// MODULE: main(lib)
// FILE: main.kt
// MODULE_KIND: AMD

fun box(): String {
    val foo = fooF()
    if (foo != "foo") return "fail1: $foo"

    val bar = barF()
    if (bar != "bar") return "fail2: $bar"

    return "OK"
}

