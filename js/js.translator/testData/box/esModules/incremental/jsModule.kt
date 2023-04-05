// SKIP_IR_INCREMENTAL_CHECKS
// DONT_TARGET_EXACT_BACKEND: JS
// ES_MODULES

// MODULE: lib
// FILE: a.kt
@file:JsModule("./foo.mjs")

external fun fooF(): String

// FILE: b.kt
@file:JsModule("./bar.mjs")

external fun barF(): String

// FILE: c.kt
// RECOMPILE
fun dummyF() = "dummy"

// MODULE: main(lib)
// FILE: main.kt

fun box(): String {
    val foo = fooF()
    if (foo != "foo") return "fail1: $foo"

    val bar = barF()
    if (bar != "bar") return "fail2: $bar"

    return "OK"
}


// FILE: foo.mjs
export function fooF() {
    return "foo";
}

// FILE: bar.mjs
export function barF() {
    return "bar";
}
