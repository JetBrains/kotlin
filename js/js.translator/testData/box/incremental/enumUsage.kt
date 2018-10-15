// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1230
// GENERATE_SOURCE_MAPS
// FILE: Enum.kt

enum class Enum {
    A,
    B
}

// FILE: usage.kt
// RECOMPILE

fun box(): String {
    println(Enum.A)

    return "OK"
}