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
void runInNewThread(void(*callback)(void));

// FILE: threadStates.cpp
#include <thread>

extern "C" void runInNewThread(void(*callback)(void)) {
    std::thread t([callback]() {
        callback();
    });
    t.join();
}

// MODULE: main(cinterop)
// FILE: main.kt
@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class, kotlinx.cinterop.ExperimentalForeignApi::class)

import kotlin.native.runtime.Debugging
import kotlin.test.*
import kotlinx.cinterop.*
import threadStates.*

fun box(): String {
    runInNewThread(staticCFunction { ->
        assertRunnableThreadState()
    })
    return "OK"
}

fun assertRunnableThreadState() {
    assertTrue(Debugging.isThreadStateRunnable)
}
