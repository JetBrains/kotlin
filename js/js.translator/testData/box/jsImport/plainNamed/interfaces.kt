// DONT_TARGET_EXACT_BACKEND: JS
// IGNORE_BACKEND: WASM
// MODULE_KIND: AMD
// FILE: bar.kt
package foo

@JsImport("lib")
external interface Bar {
    fun ping(): String
}

// FILE: baz.kt
package boo

@JsImport("lib")
external interface Baz {
    fun pong(): Int
}

// FILE: root.kt
import foo.Bar
import boo.Baz

@JsImport("lib")
external val bar: Bar

@JsImport("lib")
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