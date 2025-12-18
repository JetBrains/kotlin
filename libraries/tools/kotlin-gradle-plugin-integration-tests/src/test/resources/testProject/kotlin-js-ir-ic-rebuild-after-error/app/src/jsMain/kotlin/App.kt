/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.example

private fun someFunction(x: Any? = null) {}

fun getSomething(x: dynamic) = with(x) {
    someFunction(::unknownFunction)
    "Hello, World!"
}

fun main() {
    println(getSomething(null))
}
