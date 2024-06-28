// TARGET_BACKEND: JS_IR
// IGNORE_BACKEND: WASM
// ES_MODULES
// PROPERTY_LAZY_INITIALIZATION

// FILE: lib.kt
var z1 = false
var z2 = false

// FILE: lib2.kt

@OptIn(kotlin.ExperimentalStdlibApi::class)
@EagerInitialization
val x = run { z1 = true; 42 }

// Won't be initialized (cause no function from the file will be called during [x] initialization).
val y = run { z2 = true; 117 }

// FILE: main.kt

fun box(): String {
    return if (z1 && !z2) "OK" else "fail"
}
