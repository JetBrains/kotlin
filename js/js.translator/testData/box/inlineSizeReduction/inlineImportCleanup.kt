// EXPECTED_REACHABLE_NODES: 487
// MODULE: lib
// FILE: lib.kt
package lib

inline fun foo(f: () -> String) = f()

// MODULE: main(lib)
// FILE: main.kt
// PROPERTY_NOT_READ_FROM: foo_h4ejuu$
fun box() = lib.foo { "OK" }