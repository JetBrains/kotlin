// EXPECTED_REACHABLE_NODES: 991
// FILE: a.kt

inline fun baz(): String =
        try {
            "OK"
        }
        catch (e: Exception) {
            "not OK"
        }

// FILE: b.kt
// RECOMPILE

fun box(): String {
    return baz()
}