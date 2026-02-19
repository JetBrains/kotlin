// NO_COMMON_FILES

// MODULE: lib
// JS_MODULE_KIND: AMD
// FILE: a.kt
@file:JsModule("foo")

external fun fooF(): String

// FILE: b.kt
@file:JsModule("bar")

external fun barF(): String

// FILE: c.kt
// RECOMPILE
fun dummyF() = "dummy"

// MODULE: main(lib)
// JS_MODULE_KIND: AMD
// FILE: main.kt

fun box(): String {
    val foo = fooF()
    if (foo != "foo") return "fail1: $foo"

    val bar = barF()
    if (bar != "bar") return "fail2: $bar"

    return "OK"
}

