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
#include <stdint.h>

void assertNativeThreadState();

void runCallback(void(*callback)(void)) {
    assertNativeThreadState();
    callback();
    assertNativeThreadState();
}

int32_t answer() {
    assertNativeThreadState();
    return 42;
}

void runInNewThread(void(*callback)(void));
void runInForeignThread(void(*callback)(void));

// FILE: threadStates.cpp
#include <future>
#include <thread>
#include <stdint.h>
#include <stdlib.h>

// Implemented in the runtime for test purposes.
extern "C" bool Kotlin_Debugging_isThreadStateNative();

extern "C" void assertNativeThreadState() {
    if (!Kotlin_Debugging_isThreadStateNative()) {
        printf("Incorrect thread state. Expected native thread state.");
        abort();
    }
}

extern "C" void runInNewThread(void(*callback)(void)) {
    std::thread t([callback]() {
        callback();
    });
    t.join();
}

extern "C" void runInForeignThread(void(*callback)(void)) {
    std::thread t([callback]() {
        // This thread is not attached to the Kotlin runtime.
        auto future = std::async(std::launch::async, callback);

        // The machinery of the direct interop doesn't filter out a Kotlin exception thrown by the callback.
        // The get() call will re-throw this exception.
        future.get();
    });
    t.join();
}

// MODULE: main(cinterop)
// FILE: main.kt
@file:OptIn(FreezingIsDeprecated::class,
            kotlin.experimental.ExperimentalNativeApi::class,
            kotlin.native.runtime.NativeRuntimeApi::class,
            kotlinx.cinterop.ExperimentalForeignApi::class)

import kotlin.native.concurrent.*
import kotlin.native.runtime.Debugging
import kotlinx.cinterop.staticCFunction
import threadStates.*

fun main() {
    val hook = { throwable: Throwable ->
        print("${throwable::class.simpleName}. Runnable state: ${Debugging.isThreadStateRunnable}")
    }
    if (Platform.memoryModel != MemoryModel.EXPERIMENTAL) {
        hook.freeze()
    }

    setUnhandledExceptionHook(hook)

    runInForeignThread(staticCFunction<Unit> { throw Error("Error") })
}