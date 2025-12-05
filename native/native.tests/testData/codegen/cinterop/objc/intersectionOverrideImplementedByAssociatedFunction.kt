// ISSUE: KT-82829
// TARGET_BACKEND: NATIVE
// DISABLE_NATIVE: isAppleTarget=false

// MODULE: cinterop
// FILE: lib.def
language = Objective-C
headers = lib.h
headerFilter = lib.h

// FILE: lib.h
#include <Foundation/NSObject.h>

@protocol CoordinateSpace <NSObject>
- (int)bounds;
@end

@protocol DynamicItem <NSObject>
- (int)bounds;
@end

@interface View : NSObject <DynamicItem, CoordinateSpace>
// functions `bounds` is implemented not here, but as associated function in category ViewGeometry below,
// which is tricky for intersection override builder
@end

@interface View (ViewGeometry)
- (int)bounds;
@end

// FILE: lib.m
#import "lib.h"

@implementation View (ViewGeometry)
- (int)bounds {
    return 42;
}
@end

// MODULE: main(cinterop)
// FILE: main.kt
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
import lib.*

class Kt82829: View() {}

fun box() = "OK"

