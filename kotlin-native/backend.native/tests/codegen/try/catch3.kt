/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.`try`.catch3

import kotlin.test.*

@Test fun runTest() {
    try {
        println("Before")
        throw Error("Error happens")
        println("After")
    } catch (e: Throwable) {
        println("Caught Throwable")
    }

    println("Done")
}