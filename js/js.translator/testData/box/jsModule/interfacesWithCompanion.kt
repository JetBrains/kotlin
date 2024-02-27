// TARGET_BACKEND: JS_IR
// TARGET_BACKEND: JS_IR_ES6
// EXPECTED_REACHABLE_NODES: 1238
// MODULE_KIND: AMD
// FILE: bar.kt
@file:JsModule("bar")
package bar

external interface Bar {
    companion object {
        fun ok(): String
    }
}

// FILE: test.kt
import bar.Bar

inline fun Bar.Companion.test() = "CHECK"

fun box(): String {
    Bar.test()
    return Bar.ok()
}