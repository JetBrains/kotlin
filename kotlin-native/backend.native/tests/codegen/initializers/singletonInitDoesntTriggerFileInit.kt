/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// FILE: lib.kt
object Foo {
    val str = buildString {
        append(42)
    }
}

val fooStr = Foo.str

// FILE: main.kt
import kotlin.test.*

fun main() {
    assertEquals("42", Foo.str)
    assertEquals("42", fooStr)
}
