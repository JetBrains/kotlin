/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
// TARGET_BACKEND: NATIVE
// NATIVE_STANDALONE
// MODULE: cinterop
// FILE: threadStates.def
language = C
---
#include <stdio.h>
#include <stdlib.h>

void assertNativeThreadState() {
    // Implemented in the runtime for test purposes.
    _Bool Kotlin_Debugging_isThreadStateNative(void);

    if (!Kotlin_Debugging_isThreadStateNative()) {
        printf("Incorrect thread state. Expected native thread state.");
        abort();
    }
}

void runCallback(void(*callback)(void)) {
    assertNativeThreadState();
    callback();
    assertNativeThreadState();
}

// MODULE: main(cinterop)
// FILE: main.kt
@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class, kotlinx.cinterop.ExperimentalForeignApi::class)

import kotlin.native.runtime.Debugging
import kotlin.test.*
import kotlinx.cinterop.staticCFunction
import threadStates.*

fun box(): String {
    try {
        runCallback(staticCFunction { ->
            assertRunnableThreadState()
            runCallback(staticCFunction(::throwException))
        })
    } catch (e: CustomException) {
        assertRunnableThreadState()
        return "OK"
    } catch (e: Throwable) {
        assertRunnableThreadState()
        fail("Wrong exception type: ${e.message}")
    }
    fail("No exception thrown")
}

fun assertRunnableThreadState() {
    assertTrue(Debugging.isThreadStateRunnable)
}

class CustomException() : Exception()

fun throwException() {
    assertRunnableThreadState()
    throw CustomException()
}
