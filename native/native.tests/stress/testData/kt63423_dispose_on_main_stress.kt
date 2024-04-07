/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
// KIND: STANDALONE_NO_TR
// Test depends on macOS-specific AppKit
// DISABLE_NATIVE: isAppleTarget=false
// DISABLE_NATIVE: targetFamily=IOS
// DISABLE_NATIVE: targetFamily=TVOS
// DISABLE_NATIVE: targetFamily=WATCHOS
// requires some GC and its careful timing
// DISABLE_NATIVE: gcType=NOOP
// DISABLE_NATIVE: gcScheduler=AGGRESSIVE
// KT-65261: TODO
// DISABLE_NATIVE: useThreadStateChecker=ENABLED

// FREE_CINTEROP_ARGS: -Xsource-compiler-option -ObjC++
// FREE_COMPILER_ARGS: -Xbinary=gcSchedulerType=manual -opt-in=kotlin.native.internal.InternalForKotlinNative
// MODULE: cinterop
// FILE: objclib.def
language = Objective-C
headers = objclib.h
linkerOpts = -framework AppKit

// FILE: objclib.h
#include <objc/NSObject.h>

@interface OnDestroyHook : NSObject
- (instancetype)init;
@end

#ifdef __cplusplus
extern "C" {
    #endif

    void startApp(void (^task)());
    BOOL isMainThread();
    void spin();

    #ifdef __cplusplus
}
#endif

// FILE: objclib.m
#include "objclib.h"

#include <cinttypes>
#include <dispatch/dispatch.h>
#include <map>
#import <AppKit/NSApplication.h>
#import <Foundation/NSRunLoop.h>
#import <Foundation/NSThread.h>

std::map<uintptr_t, bool> dictionary;

@implementation OnDestroyHook
- (instancetype)init {
    if (self = [super init]) {
        dictionary[(uintptr_t)self] = true;
    }
    return self;
}

- (void)dealloc {
    dictionary[(uintptr_t)self] = false;
}

@end

extern "C" void startApp(void (^task)()) {
    dispatch_async(dispatch_get_main_queue(), ^{
        // At this point all other scheduled main queue tasks were already executed.
        // Executing via performBlock to allow a recursive run loop in `spin()`.
        [[NSRunLoop currentRunLoop] performBlock:^{
        task();
        [NSApp terminate:NULL];
    }];
    });
    [[NSApplication sharedApplication] run];
}

extern "C" BOOL isMainThread() {
    return [NSThread isMainThread];
}

extern "C" void spin() {
    if ([NSRunLoop currentRunLoop] != [NSRunLoop mainRunLoop]) {
        fprintf(stderr, "Must spin main run loop\n");
        exit(1);
    }
    while (true) {
        [[NSRunLoop currentRunLoop] runMode:NSDefaultRunLoopMode beforeDate:[NSDate dateWithTimeIntervalSinceNow:0.1]];
        bool done = true;
        for (auto kvp : dictionary) {
            if (kvp.second) {
                done = false;
                break;
            }
        }
        if (done) return;
    }
}

// MODULE: main(cinterop)
// FILE: main.kt
@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class, kotlinx.cinterop.ExperimentalForeignApi::class)

import objclib.*

import kotlin.native.internal.MemoryUsageInfo
import kotlin.test.*
import kotlinx.cinterop.*

class PeakRSSChecker(private val rssDiffLimitBytes: Long) {
    // On Linux, the child process might immediately commit the same amount of memory as the parent.
    // So, measure difference between peak RSS measurements.
    private val initialBytes = MemoryUsageInfo.peakResidentSetSizeBytes.also {
        check(it != 0L) { "Error trying to obtain peak RSS. Check if current platform is supported" }
    }

    fun check(): Long {
        val diffBytes = MemoryUsageInfo.peakResidentSetSizeBytes - initialBytes
        check(diffBytes <= rssDiffLimitBytes) { "Increased peak RSS by $diffBytes bytes which is more than $rssDiffLimitBytes" }
        return diffBytes
    }
}

fun alloc(): Unit = autoreleasepool {
    OnDestroyHook()
    Unit
}

fun waitDestruction() {
    assertTrue(isMainThread())
    kotlin.native.internal.GC.collect()
    spin()
}

fun main() = startApp {
    repeat(500000) {
        alloc()
    }
    val peakRSSChecker = PeakRSSChecker(10_000_000L) // ~10MiB allowed difference for running finalizers
    waitDestruction()
    peakRSSChecker.check()
}