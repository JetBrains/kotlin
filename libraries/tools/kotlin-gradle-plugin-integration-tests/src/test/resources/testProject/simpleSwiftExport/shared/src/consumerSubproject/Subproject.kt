package com.github.jetbrains.swiftexport

import com.github.jetbrains.library.libraryFoo
import com.github.jetbrains.library.LibFoo

fun fooFromLibrary(): Int = libraryFoo()

fun libFoo(): LibFoo {
    return LibFoo("O", 12)
}