// MODULE: lib
// FILE: lib.kt

inline fun foo(x: Char = '\u042b') = x

// MODULE: main(lib)
// FILE: main.kt

import kotlin.test.*

fun box(): String {
    assertEquals('\u042b', foo())
    return "OK"
}