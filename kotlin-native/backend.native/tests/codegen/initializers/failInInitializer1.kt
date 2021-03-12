/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// FILE: lib.kt
val x: String = computeX()

fun computeX(): String = error("zzz")

// FILE: main.kt
fun main() {
    try {
        println(x)
    } catch(t: IllegalStateException) {
        println("caught")
    }
}
