// DONT_TARGET_EXACT_BACKEND: JS
// IGNORE_BACKEND: WASM
// MODULE_KIND: AMD
// FILE: bar.kt
@file:JsImport("lib")
package foo

external interface Bar {
    fun ping(): String
}

// FILE: baz.kt
@file:JsImport("lib")
package boo

external interface Baz {
    fun pong(): Int
}

// FILE: root.kt
@file:JsImport("lib")

import foo.Bar
import boo.Baz

external val bar: Bar

external val baz: Baz

// FILE: test.kt
import boo.Baz

fun box(): String {
    if (bar.ping() != "ping" || baz.pong() != 194) return "Fail"

    val local = object : Baz {
        override fun pong(): Int = 322
    }

    if (local.asDynamic().pong() != 322) return "Fail"

    return "OK"
}