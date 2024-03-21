/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package sample

fun requireInMain() {
    val greeting = "Hello, Main!"
    require(greeting == "Hello, World!")
}

fun assertInMain() {
    val greeting = "Hello, Main!"
    assert(greeting == "Hello, World!")
}
