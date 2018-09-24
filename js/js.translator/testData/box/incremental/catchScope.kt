// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1280
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