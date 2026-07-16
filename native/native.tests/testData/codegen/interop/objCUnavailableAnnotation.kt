// TARGET_BACKEND: NATIVE
// DISABLE_NATIVE: isAppleTarget=false

// MODULE: objclib
// FILE: objclib.def
language = Objective-C
headers = objclib.h
headerFilter = objclib.h

// FILE: objclib.h
#import <Foundation/Foundation.h>

@protocol Legacy
- (int)legacyApi;
@end

@interface Widget : NSObject <Legacy>
- (int)legacyApi __attribute__((unavailable));
@end

// FILE: objclib.m
#import "objclib.h"

@implementation Widget
- (int)legacyApi { return 42; }
@end

// MODULE: main(objclib)
// FILE: main.kt
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

import objclib.Widget

@Suppress("DEPRECATION", "DEPRECATION_ERROR")
fun box(): String {
    val result = Widget().legacyApi()
    return if (result == 42) "OK" else "FAIL: legacyApi=$result"
}
