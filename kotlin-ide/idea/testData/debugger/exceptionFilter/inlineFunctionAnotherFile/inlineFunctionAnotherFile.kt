package inlineFunSameFile

import inlineFunPackage.*

fun box() {
    foo {
        println()
    }
}

// FILE: inlineFunctionFile.kt
// LINE: 4