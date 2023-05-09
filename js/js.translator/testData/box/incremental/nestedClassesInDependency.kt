// NO_COMMON_FILES
// MODULE: lib
// FILE: lib.kt

class A {
    class B(val msg: String)

    fun foo(b: B) = b.msg
}

// MODULE: main(lib)
// FILE: main.kt
// RECOMPILE

fun box() = A().foo(A.B("OK"))
