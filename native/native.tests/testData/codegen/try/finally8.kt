/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.`try`.finally8

import kotlin.test.*

@Test fun runTest() {
    println(foo())
}

fun foo(): Int {
    try {
        try {
            return 42
        } finally {
            println("Finally 1")
        }
    } finally {
        println("Finally 2")
    }

    println("After")
    return 2
}