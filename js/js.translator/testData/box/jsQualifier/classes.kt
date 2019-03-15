// EXPECTED_REACHABLE_NODES: 1282
// IGNORE_BACKEND: JS_IR
// MODULE: lib
// FILE: lib.kt
@file:JsQualifier("pkg")
external class C {
    fun o(): String

    class D {
        fun k(): String
    }
}

// MODULE: main(lib)
// FILE: main.kt

fun box() = C().o() + C.D().k()