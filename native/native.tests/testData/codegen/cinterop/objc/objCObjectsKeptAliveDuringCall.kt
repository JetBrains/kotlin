// TARGET_BACKEND: NATIVE
// DISABLE_NATIVE: isAppleTarget=false
// WITH_PLATFORM_LIBS
// IGNORE_NATIVE: gcType=NOOP

// MODULE: cinterop
// FILE: lib.def
language = Objective-C
headers = lib.h
headerFilter = lib.h

// FILE: lib.h
#import <Foundation/NSObject.h>

extern BOOL victimDeallocated;
extern BOOL sentinelDeallocated;

@interface Victim : NSObject
- (void)callBlock:(void (^)(void))block;
+ (void)callBlock:(void (^)(void))block withArgument:(__unsafe_unretained Victim*)argument;
@end

@interface Sentinel : NSObject
@end

// FILE: lib.m
#import "lib.h"

BOOL victimDeallocated = NO;
BOOL sentinelDeallocated = NO;

@implementation Victim
- (void)callBlock:(void (^)(void))block {
    block();
}

+ (void)callBlock:(void (^)(void))block withArgument:(__unsafe_unretained Victim*)argument {
    block();
}

- (void)dealloc {
    victimDeallocated = YES;
}
@end

@implementation Sentinel
- (void)dealloc {
    sentinelDeallocated = YES;
}
@end

// MODULE: main(cinterop)
// FILE: main.kt
@file:OptIn(
    kotlinx.cinterop.ExperimentalForeignApi::class,
    kotlin.native.runtime.NativeRuntimeApi::class,
    kotlin.experimental.ExperimentalNativeApi::class,
)

import lib.*
import kotlin.native.runtime.GC
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

@kotlin.native.NoInline
fun dropSentinel() {
    Sentinel()
}

fun collectGarbage() {
    sentinelDeallocated = false
    dropSentinel()
    GC.collect()
    val deadline = TimeSource.Monotonic.markNow() + 10.seconds
    while (!sentinelDeallocated) {
        check(deadline.hasNotPassedNow()) { "The sentinel has not been released" }
        platform.posix.usleep(1000u)
    }
}

fun checkVictimAlive(): String? {
    collectGarbage()
    return if (victimDeallocated) "the victim was released during the call" else null
}

fun testReceiver(): String? {
    collectGarbage()
    victimDeallocated = false
    var failure: String? = null
    Victim().callBlock { failure = checkVictimAlive() }
    return failure
}

fun testArgument(): String? {
    collectGarbage()
    victimDeallocated = false
    var failure: String? = null
    Victim.callBlock({ failure = checkVictimAlive() }, withArgument = Victim())
    return failure
}

fun box(): String {
    val failures = listOfNotNull(
            testReceiver()?.let { "receiver: $it" },
            testArgument()?.let { "argument: $it" },
    )
    return if (failures.isEmpty()) "OK" else "FAIL ${failures.joinToString("; ")}"
}
