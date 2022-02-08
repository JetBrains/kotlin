/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import kotlin.native.internal.Debugging
import kotlin.test.*
import kotlinx.cinterop.*
import threadStates.*

fun main() {
    nativeCall()
    callback()
    nestedCalls()
    directStaticCFunctionCall()
    callbackOnSeparateThread()
}

fun assertRunnableThreadState() {
    assertTrue(Debugging.isThreadStateRunnable)
}

fun nativeCall() {
    answer()
    assertRunnableThreadState()
}

fun callback() {
    runCallback(staticCFunction { ->
        assertRunnableThreadState()
    })
    assertRunnableThreadState()
}

fun nestedCalls() {
    runCallback(staticCFunction { ->
        assertRunnableThreadState()
        answer()
        Unit
    })
    assertRunnableThreadState()
}

fun directStaticCFunctionCall() {
    val funPtr = staticCFunction { ->
        assertRunnableThreadState()
    }
    assertRunnableThreadState()
    funPtr()
    assertRunnableThreadState()
}

fun callbackOnSeparateThread() {
    runInNewThread(staticCFunction { ->
        assertRunnableThreadState()
    })
}
