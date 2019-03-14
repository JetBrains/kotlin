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
    if (Enum.A.name != "A")
        return "Fail"

    return "OK"
}