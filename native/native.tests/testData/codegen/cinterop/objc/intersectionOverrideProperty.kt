// ISSUE: KT-82829
// TARGET_BACKEND: NATIVE
// DISABLE_NATIVE: isAppleTarget=false

// MODULE: cinterop
// FILE: lib.def
language = Objective-C
headers = lib.h

// FILE: lib.h
#include <Foundation/NSObject.h>

@protocol Protocol1 <NSObject>
@property NSString* ok;
@end

@protocol Protocol2 <NSObject>
@property NSString* ok;
@end

@interface MyObjCInterface : NSObject <Protocol1, Protocol2>
@property NSString* ok;
@end

// FILE: lib.m
#import "lib.h"

@implementation MyObjCInterface
- (instancetype)init {
    self = [super init];
    if (self) {
        _ok = @"OK";  // Note: use _ok (the ivar), not self.ok
    }
    return self;
}
@end

// MODULE: main(cinterop)
// FILE: main.kt
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
import lib.*

class Kt82829: MyObjCInterface() {}

fun box() = Kt82829().ok
