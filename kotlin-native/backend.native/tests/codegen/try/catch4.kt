/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.`try`.catch4

import kotlin.native.internal.*
import kotlin.test.*

@Test fun runTest() {
    val frame = runtimeGetCurrentFrame()
    try {
        assertEquals(frame, runtimeGetCurrentFrame())
        println("Before")
        throw Error("Error happens")
        println("After")
    } catch (e: Exception) {
        assertEquals(frame, runtimeGetCurrentFrame())
        println("Caught Exception")
    } catch (e: Error) {
        assertEquals(frame, runtimeGetCurrentFrame())
        println("Caught Error")
    } catch (e: Throwable) {
        assertEquals(frame, runtimeGetCurrentFrame())
        println("Caught Throwable")
    }

    println("Done")
}