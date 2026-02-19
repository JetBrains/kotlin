/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
// TARGET_BACKEND: NATIVE
// OUTPUT_REGEX: Error. Runnable state:.*
// EXIT_CODE: !0
// FREE_COMPILER_ARGS: -opt-in=kotlin.native.internal.InternalForKotlinNative
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
@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class,
            kotlin.native.runtime.NativeRuntimeApi::class,
            kotlinx.cinterop.ExperimentalForeignApi::class)

import kotlin.native.concurrent.*
import kotlin.test.*
import kotlin.native.runtime.Debugging
import kotlinx.cinterop.staticCFunction
import threadStates.*

fun main() {
    val hook = { throwable: Throwable ->
        print("${throwable::class.simpleName}. Runnable state: ${Debugging.isThreadStateRunnable}")
    }

    setUnhandledExceptionHook(hook)

    runInNewThread(staticCFunction<Unit> { throw Error("Error") })
}