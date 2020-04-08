package inlineFunCallInLibrary

import inlineFunctionCall.*

fun box() {
    call()
}

// WITH_MOCK_LIBRARY: true
// FILE: inlineFunInLibrary.kt
// LINE: 4