// EXIT_CODE: !0
// OUTPUT_REGEX: Cannot run checkers when there are 1 alive runtimes at the shutdown.*
// MODULE: cinterop
// FILE: leakMemory.def
---
void test_RunInNewThread(void (*)());

// FILE: leakMemory.h
#ifdef __cplusplus
extern "C" {
#endif

void test_RunInNewThread(void (*)());

#ifdef __cplusplus
}
#endif

// FILE: leakMemory.cpp
#include "leakMemory.h"

#include <atomic>
#include <thread>

extern "C" void test_RunInNewThread(void (*f)()) {
    std::atomic<bool> haveRun(false);
    std::thread t([f, &haveRun]() {
        f();
        haveRun = true;
        while (true) {}
    });
    t.detach();
    while (!haveRun) {}
}

// MODULE: main(cinterop)
// FILE: main.kt
@file:OptIn(
    kotlin.experimental.ExperimentalNativeApi::class,
    kotlin.native.runtime.NativeRuntimeApi::class,
    kotlinx.cinterop.ExperimentalForeignApi::class
)

import leakMemory.*
import kotlin.concurrent.AtomicInt
import kotlin.native.concurrent.*
import kotlin.native.Platform
import kotlin.test.*
import kotlinx.cinterop.*

val global = AtomicInt(0)

fun ensureInititalized() {
    // Leak memory
    StableRef.create(Any())
    global.value = 1
}

fun main() {
    Platform.isMemoryLeakCheckerActive = true
    kotlin.native.runtime.Debugging.forceCheckedShutdown = true
    assertTrue(global.value == 0)
    // Created a thread, made sure Kotlin is initialized there.
    test_RunInNewThread(staticCFunction(::ensureInititalized))
    assertTrue(global.value == 1)
    // Now exiting. With checked shutdown we will fail, complaining there're
    // unfinished threads with runtimes.
}
