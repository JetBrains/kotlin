// TARGET_BACKEND: NATIVE
// DISABLE_NATIVE: isAppleTarget=false
// FILECHECK_STAGE: CStubs

// MODULE: objclib
// FILE: objclib.def
language = Objective-C
headers = objclib.h
headerFilter = objclib.h

// FILE: objclib.h
#import <Foundation/Foundation.h>

// KotlinSubclass below implements initWithValue: only. The backend should add MissingInitImp
// for the other available initializer, but not for unavailable init.
@interface ObjCBase : NSObject
- (instancetype)init __attribute__((unavailable));
- (instancetype)initWithValue:(int)value;
- (instancetype)initWithOtherValue:(int)value;
@end

// FILE: objclib.m
#import "objclib.h"

@implementation ObjCBase
- (instancetype)initWithValue:(int)value { return self; }
- (instancetype)initWithOtherValue:(int)value { return self; }
@end

// MODULE: main(objclib)
// FILE: main.kt
@file:OptIn(kotlinx.cinterop.BetaInteropApi::class, kotlinx.cinterop.ExperimentalForeignApi::class)

import kotlinx.cinterop.ObjCObjectBase.OverrideInit
import objclib.ObjCBase
import objclib.ObjCBaseMeta

class KotlinSubclass : ObjCBase {
    @OverrideInit
    constructor(value: Int) : super(value = value)

    companion object : ObjCBaseMeta()
}

// The generated ObjC class metadata should contain exactly two instance method descriptions:
// the real Kotlin override for initWithValue: and MissingInitImp for initWithOtherValue:.
// If unavailable init were not skipped, the method description array would have three entries.
// CHECK-DAG: [[INIT_WITH_VALUE:@[0-9]+]] = internal constant [15 x i8] c"initWithValue:\00"
// CHECK-DAG: [[ENCODING:@[0-9]+]] = internal constant [11 x i8] c"@20@0:8i16\00"
// CHECK-DAG: [[INIT_WITH_OTHER_VALUE:@[0-9]+]] = internal constant [20 x i8] c"initWithOtherValue:\00"
// CHECK-DAG: = internal constant [2 x %struct.ObjCMethodDescription] [
// CHECK-DAG: %struct.ObjCMethodDescription { ptr @MissingInitImp, ptr [[INIT_WITH_OTHER_VALUE]], ptr [[ENCODING]] }
// CHECK-DAG: @"kobjcclassinfo:KotlinSubclass" = constant %struct.KotlinObjCClassInfo { ptr {{.*}}, i32 0, ptr {{.*}}, ptr {{.*}}, ptr {{.*}}, i32 2,
fun box(): String = "OK"
