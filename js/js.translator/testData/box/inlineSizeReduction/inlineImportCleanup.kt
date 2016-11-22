// MODULE: lib
// FILE: lib.kt
package lib

inline fun foo(f: () -> String) = f()

// MODULE: main(lib)
// FILE: main.kt
// PROPERTY_NOT_READ_FROM: foo_6r51u9$
fun box() = lib.foo { "OK" }