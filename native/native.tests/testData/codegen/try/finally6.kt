/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.`try`.finally6

import kotlin.test.*

@Test fun runTest() {
    println(foo())
}

fun foo(): Int {
    try {
        println("Done")
        return 0
    } finally {
        println("Finally")
        return 1
    }

    println("After")
    return 2
}