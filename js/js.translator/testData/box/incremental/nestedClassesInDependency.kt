// NO_COMMON_FILES
// MODULE: lib
// FILE: lib.kt

class A {
    class B {
        fun foo() = "OK"
    }
}

// MODULE: main(lib)
// FILE: main.kt
// RECOMPILE

fun box() = A.B().foo()