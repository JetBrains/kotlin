// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1286
// MODULE: lib
// FILE: lib.kt
fun baz(x: String) = "($x)"

inline fun foo(): String {
    return baz(bar { "OK" })
}

inline fun bar(noinline x: () -> String): String {
    return "[" + baz(boo { x() }) + "]"
}

fun boo(x: () -> String) = x()

// MODULE: main(lib)
// FILE: main.kt
fun box(): String {
    val result = foo()
    if (result != "([(OK)])") return "fail: $result"
    return "OK"
}