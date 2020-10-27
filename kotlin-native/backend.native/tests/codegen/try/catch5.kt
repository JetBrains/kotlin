/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.`try`.catch5

import kotlin.test.*

@Test fun runTest() {
    try {
        try {
            println("Before")
            foo()
            println("After")
        } catch (e: Exception) {
            println("Caught Exception")
        }

        println("After nested try")

    } catch (e: Error) {
        println("Caught Error")
    } catch (e: Throwable) {
        println("Caught Throwable")
    }

    println("Done")
}

fun foo() {
    throw Error("Error happens")
    println("After in foo()")
}