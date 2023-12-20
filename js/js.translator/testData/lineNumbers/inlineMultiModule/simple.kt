// MODULE: lib
// FILE: lib.kt

package pkg1

inline fun foo(x: String) {
    println("foo1($x);")
    println("foo2($x);")
}

// LINES(JS_IR): 6 6 7 7 8 8

// MODULE: main(lib)
// FILE: main.kt

package pkg2
import pkg1.*

fun box() {
    foo("23")
    foo("42")
}

// LINES(JS_IR): 19 19 * 7 7 8 7 8 8 * 7 7 8 7 8 8
