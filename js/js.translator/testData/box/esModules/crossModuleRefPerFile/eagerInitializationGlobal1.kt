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
val x = foo()

private fun foo(): Int {
    z1 = true
    return 42
}

// Will be initialized since [x]'s initializer calls a function from the file.
val y = run { z2 = true; 117 }

// FILE: main.kt

fun box(): String {
    return return if (z1 && z2) "OK" else "fail"
}
