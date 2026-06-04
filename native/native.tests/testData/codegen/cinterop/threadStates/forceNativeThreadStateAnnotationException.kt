/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
// IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_SECOND_STAGE: Native:2.3
// ^^^ KT-86026 TODO: Rework testdata to move functions/globals definitions from .def/.h into separate source files

// TARGET_BACKEND: NATIVE
// NATIVE_STANDALONE
// MODULE: cinterop
// FILE: threadStates.def
language = C
---
#include <stdbool.h>
#include <stdio.h>
#include <stdlib.h>

_Bool Kotlin_Debugging_isThreadStateNative(void);

void kt77616_runCallback(void (*callback)(void)) {
    if (!Kotlin_Debugging_isThreadStateNative()) {
        printf("Incorrect thread state. Expected native thread state.");
        abort();
    }

    callback();

    printf("Shouldn't be reachable. Expected an exception.");
    abort();
}

// MODULE: main(cinterop)
// OPT_IN: kotlin.native.SymbolNameIsInternal
// FILE: main.kt
@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class, kotlinx.cinterop.ExperimentalForeignApi::class)

import kotlin.native.ForceNativeThreadState
import kotlin.native.SymbolName
import kotlin.native.runtime.Debugging
import kotlin.test.*
import kotlinx.cinterop.CFunction
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.staticCFunction

@SymbolName("kt77616_runCallback")
@ForceNativeThreadState
private external fun runCallback(callback: CPointer<CFunction<() -> Unit>>)

class CustomException : Exception()

fun throwException() {
    assertRunnableThreadState()
    throw CustomException()
}

fun box(): String {
    try {
        runCallback(staticCFunction(::throwException))
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
