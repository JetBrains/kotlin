// MODULE: lib
// FILE: lib.kt

package pkg1

inline fun foo(x: String) {
    println("foo1($x);")
    println("foo2($x);")
}

// LINES(JS):    6 6 6 6 6 9 7 7 8 8
// LINES(JS_IR):       6 6   7 7 8 8

// MODULE: main(lib)
// FILE: main.kt

package pkg2
import pkg1.*

fun box() {
    foo("23")
    foo("42")
}

// LINES(JS):    6    20 23 7 7 21 7 8 8 21      8 7 7 22 7 8 8 22 8
// LINES(JS_IR):   20 20 *  7 7      8 8    *   7 7      8 8
