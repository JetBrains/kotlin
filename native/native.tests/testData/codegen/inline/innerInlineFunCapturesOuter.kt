// MODULE: lib
// FILE: lib.kt

package a

class E(val x: String) {
    inner class Inner {
        inline fun foo(y: String) = x + y
    }
}

// MODULE: main(lib)
// FILE: main.kt

import a.*

fun box(): String {
    return E("O").Inner().foo("K")
}