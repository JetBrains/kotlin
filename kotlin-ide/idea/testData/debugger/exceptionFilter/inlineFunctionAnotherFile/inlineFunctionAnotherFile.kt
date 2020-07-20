package inlineFunSameFile

import inlineFunPackage.*

fun box() {
    foo {
        println()
    }
}

// MAIN_CLASS: inlineFunSameFile.InlineFunctionAnotherFileKt
// FILE: inlineFunctionFile.kt
// LINE: 4