// MODULE: lib
// FILE: lib.kt

object O {
    fun bar() = "OK"
}

inline fun foo() = O.bar()

// MODULE: main(lib)
// FILE: main.kt

fun box() = foo()
