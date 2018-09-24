// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1283
// FILE: a.kt
inline fun foo(f: () -> String): () -> String {
    val result = f()
    return { result }
}


// FILE: main.kt
// RECOMPILE
fun bar(f: () -> String) = foo(f)()

fun box(): String = bar { "OK" }