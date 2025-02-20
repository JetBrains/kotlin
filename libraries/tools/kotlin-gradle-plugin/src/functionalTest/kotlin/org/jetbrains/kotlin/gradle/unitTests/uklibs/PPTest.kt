/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests.uklibs

import kotlin.test.Test
import kotlin.test.assertEquals

class PPTest {

    @Test
    fun test() {
        val immutable: Any = listOf("a")
        val mutable: Any = mutableListOf("m")

        fun foo(value: Any) {
            when (value) {
                is MutableList<*> -> {
                }
                is List<*> -> {
                    println("List: ${value}, ${value.javaClass}, ${List::class.java.isInstance(value)}")
                }
            }
        }

        foo(mutable)
        foo(immutable)
    }
}