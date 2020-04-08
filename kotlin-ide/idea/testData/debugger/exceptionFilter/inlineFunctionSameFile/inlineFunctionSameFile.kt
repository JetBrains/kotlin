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

// FILE: inlineFunctionSameFile.kt
// LINE: 10