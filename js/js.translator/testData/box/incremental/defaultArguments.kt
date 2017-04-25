// EXPECTED_REACHABLE_NODES: 487
// FILE: a.kt

inline fun foo(f: (Int) -> String, x: Int = 23): String = "foo(${f(x)})"


// FILE: b.kt
// RECOMPILE

fun box(): String {
    val result = foo({ it.toString() }) + foo({ it.toString() }, 42)
    if (result != "foo(23)foo(42)") return "fail: $result"

    return "OK"
}