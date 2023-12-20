// MODULE: lib
// FILE: lib1.kt

package qwerty

fun foo(a: String): String {
    return a
}

// FILE: lib2.kt

package qwerty

fun foo2(a: String): String {
    return a
}

// MODULE: main(lib)
// FILE: main.kt

import qwerty.*

fun box() = foo(foo2("OK"))