package com.github.jetbrains.swiftexport

import com.subproject.library.libraryFoo

fun foobar(param: Int): Int = foo() + bar() + param

fun fooFromLibrary(): Int = libraryFoo()

fun functionToRemove(): Int = 4444