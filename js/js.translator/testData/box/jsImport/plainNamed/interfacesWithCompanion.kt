// TARGET_BACKEND: JS_IR
// TARGET_BACKEND: JS_IR_ES6
// EXPECTED_REACHABLE_NODES: 1238
// MODULE_KIND: AMD
// FILE: bar.kt
package bar

@JsImport("lib")
external interface Bar {
    companion object {
        fun ok(): String
    }
}

// FILE: test.kt
import bar.Bar

fun box(): String {
    return Bar.ok()
}