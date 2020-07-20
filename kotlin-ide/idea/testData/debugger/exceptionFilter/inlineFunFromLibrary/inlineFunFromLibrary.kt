package inlineFunFromLibrary

import inlineFunInLibrary.*

fun box() {
    foo {
        println()
    }
}

// MAIN_CLASS: inlineFunFromLibrary.InlineFunFromLibraryKt
// FILE: inlineFunInLibrary.kt
// LINE: 4