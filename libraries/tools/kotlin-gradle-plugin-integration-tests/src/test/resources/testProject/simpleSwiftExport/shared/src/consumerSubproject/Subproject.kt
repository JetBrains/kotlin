package com.github.jetbrains.swiftexport

import com.subproject.library.libraryFoo
import com.subproject.library.LibFoo

fun fooFromLibrary(): Int = libraryFoo()

fun libFoo(): LibFoo {
    return LibFoo("O", 12)
}