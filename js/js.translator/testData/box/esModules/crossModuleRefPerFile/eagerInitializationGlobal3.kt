// TARGET_BACKEND: JS_IR
// IGNORE_BACKEND: WASM
// ES_MODULES
// PROPERTY_LAZY_INITIALIZATION

// FILE: lib.kt
var z1 = false

// FILE: lib2.kt

@OptIn(kotlin.ExperimentalStdlibApi::class)
@EagerInitialization
val x = run { z1 = !z1; 42 }

val y = run { 73 }

fun box(): String {
    return if (z1) "OK" else "fail"
}
