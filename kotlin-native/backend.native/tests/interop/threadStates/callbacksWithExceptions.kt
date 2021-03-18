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
    nestedCallbackWithException()
    nestedCallbackWithFinally()
}

fun assertRunnableThreadState() {
    assertTrue(Debugging.isThreadStateRunnable)
}

class CustomException() : Exception()

fun throwExcetion() {
    assertRunnableThreadState()
    throw CustomException()
}

fun callbackWithException() {
    try {
        runCallback(staticCFunction(::throwExcetion))
    } catch (e: CustomException) {
        assertRunnableThreadState()
        return
    } catch (e: Throwable) {
        assertRunnableThreadState()
        fail("Wrong exception type: ${e.message}")
    }
    fail("No excetion thrown")
}

fun callbackWithFinally() {
    try {
        runCallback(staticCFunction(::throwExcetion))
    } catch (e: CustomException) {
        assertRunnableThreadState()
        return
    } finally {
        assertRunnableThreadState()
    }
    assertRunnableThreadState()
}

fun nestedCallbackWithException() {
    try {
        runCallback(staticCFunction { ->
            assertRunnableThreadState()
            runCallback(staticCFunction(::throwExcetion))
        })
    } catch (e: CustomException) {
        assertRunnableThreadState()
        return
    } catch (e: Throwable) {
        assertRunnableThreadState()
        fail("Wrong exception type: ${e.message}")
    }
    fail("No excetion thrown")
}

fun nestedCallbackWithFinally() {
    try {
        runCallback(staticCFunction { ->
            assertRunnableThreadState()
            runCallback(staticCFunction(::throwExcetion))
        })
    } catch (e: CustomException) {
        assertRunnableThreadState()
        return
    } finally {
        assertRunnableThreadState()
    }
    assertRunnableThreadState()
}