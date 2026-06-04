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

int kt77616_functionWithoutAnnotation(void) {
    if (Kotlin_Debugging_isThreadStateNative()) {
        printf("Incorrect thread state. Expected runnable thread state.");
        abort();
    }
    return 43;
}

int kt77616_functionWithAnnotation(void) {
    if (!Kotlin_Debugging_isThreadStateNative()) {
        printf("Incorrect thread state. Expected native thread state.");
        abort();
    }
    return 42;
}

// MODULE: main(cinterop)
// OPT_IN: kotlin.native.SymbolNameIsInternal
// FILE: main.kt
@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class)

import kotlin.native.ForceNativeThreadState
import kotlin.native.SymbolName
import kotlin.native.runtime.Debugging
import kotlin.test.*

@SymbolName("kt77616_functionWithoutAnnotation")
private external fun functionWithoutAnnotation(): Int

@SymbolName("kt77616_functionWithAnnotation")
@ForceNativeThreadState
private external fun functionWithAnnotation(): Int

fun box(): String {
    assertRunnableThreadState()
    assertEquals(43, functionWithoutAnnotation())
    assertRunnableThreadState()
    assertEquals(42, functionWithAnnotation())
    assertRunnableThreadState()
    return "OK"
}

fun assertRunnableThreadState() {
    assertTrue(Debugging.isThreadStateRunnable)
}
