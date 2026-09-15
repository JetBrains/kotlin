// ISSUE: KT-89363
// DONT_TARGET_EXACT_BACKEND: WASM_JS
// WASM_MUTE_REASON: UNSUPPORTED_JS_INTEROP

// FILE: main.kt
open external class Base() {
    companion object
}

class Derived : Base() {
    companion object {
        val ok = "OK"
    }
}

fun box(): String {
    return Derived.ok
}

// FILE: Base.js

function Base() {}
