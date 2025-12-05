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
@property (nonatomic, readonly) int bounds;
@end

@protocol DynamicItem <NSObject>
@property (nonatomic, readonly) int bounds;
@end

@interface View : NSObject <DynamicItem, CoordinateSpace>
// property `bounds` is implemented not here, but as associated property in category ViewGeometry below,
// which is tricky for intersection override builder
@end

@interface View(ViewGeometry)
@property(nonatomic, readonly) int bounds;
@end

// FILE: lib.m
#import "lib.h"

@implementation View
@end

// MODULE: main(cinterop)
// FILE: main.kt
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
import lib.*

class Kt82829: View() {}

fun box() = "OK"

