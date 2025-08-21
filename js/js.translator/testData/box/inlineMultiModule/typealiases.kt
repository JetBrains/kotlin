// MODULE: lib
// FILE: lib.kt

package lib

object O {
    val x = "O"

    val y = "K"
}

class C {
    @Suppress("TOPLEVEL_TYPEALIASES_ONLY", "UNSUPPORTED_FEATURE")
    typealias B = O
}

typealias A = O

// MODULE: main(lib)
// FILE: main.kt

package foo

import lib.*

fun box() = A.x + C.B.y
