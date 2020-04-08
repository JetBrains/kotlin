package inlineFunInnerClassFromLibrary

fun box() {
    inlineFunctionInnerClassInLbrary.A().Inner().test()
}

// WITH_MOCK_LIBRARY: true
// FILE: inlineFunInLibrary.kt
// LINE: 4