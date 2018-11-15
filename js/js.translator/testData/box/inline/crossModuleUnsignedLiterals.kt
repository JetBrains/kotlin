// KJS_WITH_FULL_RUNTIME
// EXPECTED_REACHABLE_NODES: 1625
// FILE: lib.kt


inline fun tenUInt() = 10U

inline fun tenULong() = 10UL

// FILE: main.kt
// RECOMPILE

fun box(): String {

    if (tenUInt() != 10U) return "fail 1"

    if (tenULong() != 10UL) return "fail 2"

    return "OK"
}