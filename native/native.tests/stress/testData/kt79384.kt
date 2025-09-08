// KIND: STANDALONE_NO_TR
// DISABLE_NATIVE: isAppleTarget=false
// forceNativeThreadStateForFunctions binary option is incompatible with caches.
// IGNORE_NATIVE: cacheMode=STATIC_EVERYWHERE
// IGNORE_NATIVE: cacheMode=STATIC_ONLY_DIST
// IGNORE_NATIVE: cacheMode=STATIC_PER_FILE_EVERYWHERE
// FREE_COMPILER_ARGS: -opt-in=kotlin.native.SymbolNameIsInternal -Xbinary=forceNativeThreadStateForFunctions=dereferenceWeak

// MODULE: cinterop
// FILE: cinterop.def
language = Objective-C
headers = cinterop.h

// FILE: cinterop.h
#import <Foundation/Foundation.h>

#ifdef __cplusplus
extern "C" {
#endif

void prepare(id objTemplate, void (^threadInit)());
void checkpoint(void);

#ifdef __cplusplus
}
#endif

// FILE: cinterop.mm
#import "cinterop.h"

#include <chrono>
#include <cstdint>
#include <iostream>

namespace {

constexpr int OBJECT_COUNT = 1000;
constexpr int RUN_LOOP_COUNT = 10;

id objs[OBJECT_COUNT];
__weak id weakObjs[OBJECT_COUNT];

constexpr auto maxCheckpointWaitTime = std::chrono::seconds(10);
constexpr auto maxTestDuration = std::chrono::seconds(30);

std::chrono::steady_clock::time_point startedAt;
std::atomic<std::chrono::steady_clock::time_point> checkpointAt;

std::atomic<uint64_t> sink = 0;

void (^threadInit)() = nullptr;

void derefWeak(uint64_t index) {
    if (weakObjs[index % OBJECT_COUNT])
        sink.fetch_add(1, std::memory_order_relaxed);
}

void checkpointChecker() {
    auto now = std::chrono::steady_clock::now();
    if (now - checkpointAt.load() > maxCheckpointWaitTime) {
        std::cerr << "Timeout waiting for checkpoint\n";
        std::abort();
    }
    auto checkerInterval = std::chrono::nanoseconds(maxCheckpointWaitTime) / 2;
    dispatch_after(dispatch_time(DISPATCH_TIME_NOW, checkerInterval.count()),
            dispatch_get_global_queue(QOS_CLASS_USER_INITIATED, 0), ^{
                checkpointChecker();
            });
}

void runLoop(uint64_t index, bool doThreadInit) {
    if (doThreadInit)
        threadInit();
    derefWeak(index);
    dispatch_async(dispatch_get_global_queue(QOS_CLASS_BACKGROUND, 0), ^{ runLoop(index + 1, doThreadInit); });
}

}

extern "C" void prepare(id objTemplate, void (^threadInit)()) {
    ::threadInit = threadInit;
    Class kClass = [objTemplate class];
    for (int i = 0; i < OBJECT_COUNT; ++i) {
        id obj = i % 2 ? [kClass new] : [NSObject new];
        objs[i] = obj;
        weakObjs[i] = obj;
    }
    for (int i = 0; i < RUN_LOOP_COUNT; ++i) {
        // Spread starting indices.
        uint64_t index = OBJECT_COUNT / RUN_LOOP_COUNT * i + (i % 2); 
        // TODO(KT-80770): `threadInit` is being retained in `runLoop` in a runnable state, which may cause a deadlock.
        //                 Fix this and then replace `false` with `i % 2` or similar.
        bool doThreadInit = false;
        runLoop(index, doThreadInit); 
    }
    startedAt = std::chrono::steady_clock::now();
    checkpointAt = startedAt;
    checkpointChecker();
}

extern "C" void checkpoint(void) {
    auto now = std::chrono::steady_clock::now();
    checkpointAt = now;
    auto testDuration = now - startedAt;
    if (testDuration >= maxTestDuration) {
        std::cerr << "Done: " << sink << "\n";
        std::exit(0);
    }
}

// Used by Kotlin via SymbolName.
extern "C" void dereferenceWeak(uint64_t index) {
    derefWeak(index * 2); // Obj-C objects are on even places, can't dereference Kotlin object, because thread state assertion will fail
}

// MODULE: main(cinterop)
// FILE: main.kt
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlin.native.runtime.NativeRuntimeApi::class)

import cinterop.*
import kotlin.time.Duration.Companion.microseconds

@SymbolName("dereferenceWeak")
private external fun dereferenceWeak(index: ULong)

fun main() {
    // Force GC to run almost non-stop.
    kotlin.native.runtime.GC.regularGCInterval = 10.microseconds
    kotlin.native.runtime.GC.collect() // to apply the new GC interval immediately.
    prepare(Any()) {}
    var index = 0UL
    while(true) {
        dereferenceWeak(index++)
        // checkpoint will exit itself, when enough time has passed.
        checkpoint()
    }
}
