// TARGET_BACKEND: NATIVE
// DISABLE_NATIVE: isAppleTarget=false
// IGNORE_KLIB_RUNTIME_ERRORS_WITH_CUSTOM_FIRST_STAGE: Native:1.9,2.0,2.1,2.2,2.3,2.4
// IGNORE_KLIB_RUNTIME_ERRORS_WITH_CUSTOM_SECOND_STAGE: Native:1.9,2.0,2.1,2.2,2.3,2.4
// ^^^KT-77446 is fixed in 2.4.20-Beta1

// KT-77446: a Kotlin class subclassing an Objective-C class whose -init is declared unavailable
// must not crash when constructed via another initializer that internally dispatches through
// [self init]. AVPlayerItem.initWithAsset:automaticallyLoadedAssetKeys: is one real-world case;
// this test reproduces the same shape with a local ObjC base class so the test does not depend
// on AVFoundation host behavior.

// MODULE: objclib
// FILE: objclib.def
language = Objective-C
headers = objclib.h
headerFilter = objclib.h

// FILE: objclib.h
#import <Foundation/Foundation.h>

@interface ObjCBase : NSObject
- (instancetype)init __attribute__((unavailable));
- (instancetype)initWithValue:(int)value;
@property (nonatomic, readonly) int storedValue;
@end

// FILE: objclib.m
#import "objclib.h"

@implementation ObjCBase {
    int _storedValue;
}

- (instancetype)init {
    return [super init];
}

- (instancetype)initWithValue:(int)value {
    self = [self init]; // This is the path that hit MissingInitImp before KT-77446 was fixed.
    if (self) {
        _storedValue = value;
    }
    return self;
}

- (int)storedValue {
    return _storedValue;
}

@end

// MODULE: main(objclib)
// FILE: main.kt
@file:OptIn(kotlinx.cinterop.BetaInteropApi::class, kotlinx.cinterop.ExperimentalForeignApi::class)

import kotlinx.cinterop.ObjCObjectBase.OverrideInit
import objclib.ObjCBase

class KotlinSubclass : ObjCBase {
    @OverrideInit
    constructor(value: Int) : super(value = value)
}

fun box(): String {
    val obj = KotlinSubclass(value = 42)
    if (obj.storedValue() != 42) return "FAIL: storedValue=${obj.storedValue()}"
    return "OK"
}
