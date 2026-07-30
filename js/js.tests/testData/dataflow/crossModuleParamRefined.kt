// MODULE: lib
// FILE: lib.kt
interface Base
class Child : Base

fun take(p: Base): String = if (p is Child) "OK" else "FAIL"

// MODULE: main(lib)
// FILE: main.kt
// Precision: cross-module monomorphic call refines lib.take parameter.

fun box(): String = take(Child())
