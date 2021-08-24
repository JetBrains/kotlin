/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.`try`.finally4

import kotlin.native.internal.*
import kotlin.test.*

@Test fun runTest() {
    val frame = runtimeGetCurrentFrame()
    try {
        try {
            println("Try")
            throw Error("Error happens")
            println("After throw")
        } catch (e: Error) {
            assertTrue(runtimeCurrentFrameIsEqual(frame))
            println("Catch")
            throw Exception()
            println("After throw")
        } finally {
            println("Finally")
        }

        println("After nested try")

    } catch (e: Error) {
        assertTrue(runtimeCurrentFrameIsEqual(frame))
        println("Caught Error")
    } catch (e: Exception) {
        assertTrue(runtimeCurrentFrameIsEqual(frame))
        println("Caught Exception")
    }

    println("Done")
}