package inlineFunSameFile

fun box() {
    foo {
        println()
    }
}

inline fun foo(f: () -> Unit) {
    null!!
    f()
}

// MAIN_CLASS: inlineFunSameFile.InlineFunctionSameFileKt
// FILE: inlineFunctionSameFile.kt
// LINE: 10