/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.`try`.finally3

import kotlin.test.*

@Test fun runTest() {

    try {
        try {
            println("Try")
            throw Error("Error happens")
            println("After throw")
        } finally {
            println("Finally")
        }

        println("After nested try")

    } catch (e: Error) {
        println("Caught Error")
    }

    println("Done")
}