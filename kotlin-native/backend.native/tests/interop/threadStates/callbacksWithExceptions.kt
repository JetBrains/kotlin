/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import kotlin.native.internal.Debugging
import kotlin.test.*
import kotlinx.cinterop.staticCFunction
import threadStates.*

fun main() {
    callbackWithException()
    callbackWithFinally()
    callbackWithFinallyNoCatch()
    nestedCallbackWithException()
    nestedCallbackWithFinally()
}

fun assertRunnableThreadState() {
    assertTrue(Debugging.isThreadStateRunnable)
}

class CustomException() : Exception()

fun throwException() {
    assertRunnableThreadState()
    throw CustomException()
}

fun callbackWithException() {
    val frame = runtimeGetCurrentFrame()
    try {
        runCallback(staticCFunction(::throwException))
    } catch (e: CustomException) {
        assertEquals(frame, runtimeGetCurrentFrame())
        assertRunnableThreadState()
        return
    } catch (e: Throwable) {
        assertEquals(frame, runtimeGetCurrentFrame())
        assertRunnableThreadState()
        fail("Wrong exception type: ${e.message}")
    }
    fail("No exception thrown")
}

fun callbackWithFinally() {
    val frame = runtimeGetCurrentFrame()
    try {
        runCallback(staticCFunction(::throwException))
    } catch (e: CustomException) {
        assertEquals(frame, runtimeGetCurrentFrame())
        assertRunnableThreadState()
        return
    } finally {
        assertRunnableThreadState()
    }
    fail("No exception thrown")
}

fun callbackWithFinallyNoCatch() {
    val frame = runtimeGetCurrentFrame()
    try {
        try {
            runCallback(staticCFunction(::throwException))
        } finally {
            assertRunnableThreadState()
        }
        assertRunnableThreadState()
    } catch (_: CustomException) {
        assertEquals(frame, runtimeGetCurrentFrame())
    }
}

fun nestedCallbackWithException() {
    val frame = runtimeGetCurrentFrame()
    try {
        runCallback(staticCFunction { ->
            assertRunnableThreadState()
            runCallback(staticCFunction(::throwException))
        })
    } catch (e: CustomException) {
        assertEquals(frame, runtimeGetCurrentFrame())
        assertRunnableThreadState()
        return
    } catch (e: Throwable) {
        assertEquals(frame, runtimeGetCurrentFrame())
        assertRunnableThreadState()
        fail("Wrong exception type: ${e.message}")
    }
    fail("No exception thrown")
}

fun nestedCallbackWithFinally() {
    val frame = runtimeGetCurrentFrame()
    try {
        runCallback(staticCFunction { ->
            assertRunnableThreadState()
            runCallback(staticCFunction(::throwException))
        })
    } catch (e: CustomException) {
        assertEquals(frame, runtimeGetCurrentFrame())
        assertRunnableThreadState()
        return
    } finally {
        assertRunnableThreadState()
    }
    fail("No exception thrown")
}