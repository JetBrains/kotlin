package inlineFunSameFile

import inlineFunPackage.*

fun box() {
    foo {
        println()
    }
}

// MAIN_CLASS: inlineFunSameFile.InlineFunctionAnotherFileWithSmapAppliedKt
// SMAP_APPLIED
// FILE: inlineFunctionFile.kt
// LINE: 4