// TARGET_BACKEND: NATIVE
// DISABLE_NATIVE: isAppleTarget=false
// IGNORE_NATIVE: gcType=NOOP

// MODULE: cinterop
// FILE: lib.def
language = Objective-C
headers = lib.h
headerFilter = lib.h

// FILE: lib.h
#include <objc/objc.h>
#include <Foundation/NSObject.h>

@interface BooleanFlag : NSObject
@property BOOL value;
@end

@interface SetFlagOnDealloc : NSObject
@property BooleanFlag* flag;
@end

static void* getDeallocFlagSettingBlock(BooleanFlag* flag) {
    SetFlagOnDealloc* obj = [SetFlagOnDealloc new];
    obj.flag = flag;
    return (__bridge_retained void*)^SetFlagOnDealloc* (void) {
        return obj; // Captured => the object should be released only after the (copied) block is removed.
    };
}

// FILE: lib.m
#import "lib.h"

@implementation BooleanFlag
@end

@implementation SetFlagOnDealloc
- (void)dealloc {
    self.flag.value = YES;
}
@end

// MODULE: main(cinterop)
// FILE: main.kt
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
import lib.*
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

// This function runs [block] and checks that it didn't use an autoreleasepool.
inline fun <R> noAutorelease(block: () -> R): R {
    // Just in case our magic doesn't work, let's at least flush autoreleasepool after the end,
    // to detect over-release via autorelease.
    autoreleasepool {
        val poolBefore = objc_autoreleasePoolPush()
        objc_autoreleasePoolPop(poolBefore)

        val result = block()

        val poolAfter = objc_autoreleasePoolPush()
        objc_autoreleasePoolPop(poolAfter)

        // autoreleasepool machinery is implemented as a stack.
        // So here we check that stack top after `block()` is the same as before.
        // Which means that no objects were added to this stack inbetween.
        check(poolBefore == poolAfter) { "The code used an autoreleasepool; $poolBefore -> $poolAfter" }

        return result
    }
}

@OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
fun box(): String {
    val deadObjectFlag = BooleanFlag()
    val deadBlockPtr = noAutorelease { getDeallocFlagSettingBlock(deadObjectFlag)!!.rawValue }
    if (deadObjectFlag.value) return "FAIL 1"

    objc_release(deadBlockPtr) // Balance __bridge_retained.
    if (!deadObjectFlag.value) return "FAIL 2"

    // Now do the same, but create a Kotlin function object retaining the block.

    val liveObjectFlag = BooleanFlag()
    val liveBlockPtr = noAutorelease { getDeallocFlagSettingBlock(liveObjectFlag)!!.rawValue }
    if (liveObjectFlag.value) return "FAIL 3"

    var liveBlock: (()->SetFlagOnDealloc)?
    {
        liveBlock = noAutorelease { convertBlockPtrToKotlinFunction<()->SetFlagOnDealloc>(liveBlockPtr) }
    }() // wrap to function to allow the Kotlin object to be GCed before the end of the function.

    objc_release(liveBlockPtr) // Balance __bridge_retained.
    repeat(2) { kotlin.native.runtime.GC.collect() }
    if (liveObjectFlag.value) return "FAIL 4"

    liveBlock = null
    kotlin.native.runtime.GC.collect()
    if (!liveObjectFlag.value) return "FAIL 5"

    return "OK"
}
