// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1281
// MODULE: lib
// FILE: lib1.kt

inline fun foo() = "OK"

inline fun bar(x: String) = "($x)"

// MODULE: main(lib)
// FILE: main.kt
// CHECK_NOT_CALLED: foo
// CHECK_NOT_CALLED: bar

fun box(): String {
    if (prop != "(OK)") return "fail: $prop"
    return "OK"
}

val prop = bar(foo())