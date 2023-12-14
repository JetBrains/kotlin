/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.`try`.finally2

import kotlin.test.*

@Test fun runTest() {

    try {
        println("Try")
        throw Error("Error happens")
        println("After throw")
    } catch (e: Error) {
        println("Caught Error")
    } finally {
        println("Finally")
    }

    println("Done")
}