package inlineFunInnerClassFromLibrary

fun box() {
    inlineFunctionInnerClassInLbrary.A().Inner().test()
}

// MAIN_CLASS: inlineFunInnerClassFromLibrary.InlineFunInnerClassFromLibraryKt
// FILE: inlineFunInLibrary.kt
// LINE: 4