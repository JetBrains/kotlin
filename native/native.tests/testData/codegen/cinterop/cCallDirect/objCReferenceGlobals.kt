// TARGET_BACKEND: NATIVE
// FREE_CINTEROP_ARGS: -Xccall-mode direct

// The test relies on GC actually collecting garbage and doesn't make any sense otherwise:
// IGNORE_NATIVE: gcType=NOOP

// MODULE: cinterop
// FILE: lib.def
language = Objective-C
headers = lib.h
headerFilter = lib.h

// FILE: lib.h
#import <Foundation/Foundation.h>

@interface MyInt : NSObject
@property int value;
- (instancetype)initWithValue:(int)value;
@end

extern MyInt* myInt;
extern NSArray<MyInt*>* myIntArray;
extern int deallocCount;

void init(void);

// FILE: lib.m
#include "lib.h"

@implementation MyInt
- (instancetype)initWithValue:(int)value_ {
    if (self = [super init]) {
        self.value = value_;
    }
    return self;
}

- (void)dealloc {
    deallocCount++;
}
@end

MyInt* myInt = nil;
NSArray<MyInt*>* myIntArray = nil;
int deallocCount = 0;

void init(void) {
    myInt = [[MyInt alloc] initWithValue:1];
    myIntArray = @[[[MyInt alloc] initWithValue:2], [[MyInt alloc] initWithValue:3]];
}

// MODULE: main(cinterop)
// FILE: main.kt
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

import lib.*

fun kotlinInit(value: Int) {
    myInt = MyInt(value)
}

fun getDeallocCount(): Int {
    // Garbage-collect all Kotlin wrappers of Objective-C objects,
    // so that unreachable Objective-C objects get deallocated:
    @OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
    kotlin.native.runtime.GC.collect()

    return deallocCount
}

// The function below is logically a part of the `box` function. It is extracted for a reson.
//
// The `box` function is going to assume that Kotlin wrappers of Objective-C objects are garbage-collected.
// But they can't be if they are used in the same function stack frame (KT-80598).
// So we need to move the usage (`myInt!!.value`) to a separate function.
fun box1(): String? {
    if (getDeallocCount() != 0) return "FAIL 1: $deallocCount"

    init()
    if (getDeallocCount() != 0) return "FAIL 2: $deallocCount"

    val result1 = myInt!!.value
    if (result1 != 1) return "FAIL 3: $result1"

    val result2 = myIntArray!!.map { (it as MyInt).value }
    if (result2 != listOf(2, 3)) return "FAIL 4: $result2"

    return null
}

fun box(): String {
    box1()?.let { return it }

    // Replace the first instance of MyInt:
    kotlinInit(4)
    // Check it is deallocated:
    if (getDeallocCount() != 1) return "FAIL 5: $deallocCount"

    // Release the second instance of MyInt:
    myInt = null
    // Check it is deallocated:
    if (getDeallocCount() != 2) return "FAIL 6: $deallocCount"

    // Replace null with the third instance of MyInt:
    kotlinInit(5)
    // Check the new object is not deallocated and therefore properly retained:
    if (getDeallocCount() != 2) return "FAIL 7: $deallocCount"

    // Also check that assignment in `kotlinInit` changes the value:
    val result3 = myInt!!.value
    if (result3 != 5) return "FAIL 8: $result3"

    return "OK"
}
