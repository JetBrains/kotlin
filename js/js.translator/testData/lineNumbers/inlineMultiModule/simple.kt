// MODULE: lib
// FILE: lib.kt

package pkg1

inline fun foo(x: String) {
    println("foo1($x);")
    println("foo2($x);")
}

// LINES: 7 8

// MODULE: main(lib)
// FILE: main.kt

package pkg2
import pkg1.*

fun box() {
    foo("23")
    foo("42")
}

// LINES: 7 20 7 8 20 8 7 21 7 8 21 8