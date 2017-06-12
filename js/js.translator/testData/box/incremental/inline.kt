// EXPECTED_REACHABLE_NODES: 487
// FILE: a.kt

private inline fun bar(f: () -> String): String = "bar(${f()})"

internal inline fun foo(f: () -> String): String = "foo(${bar(f)})"

inline fun baz(f: () -> String): String = "baz(${f()})"

// FILE: b.kt
// RECOMPILE

fun box(): String {
    val result = foo { "O" } + baz { "K" }
    if (result != "foo(bar(O))baz(K)") return "fail: $result"

    return "OK"
}