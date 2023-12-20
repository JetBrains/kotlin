// MODULE: lib
// FILE: lib.kt

fun foo(): String {
    val bar: String by lazy {
        "OK"
    }

    return bar
}

// MODULE: main(lib)
// FILE: main.kt

import kotlin.test.*

fun box() = foo()
