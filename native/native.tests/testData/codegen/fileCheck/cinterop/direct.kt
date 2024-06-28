// TARGET_BACKEND: NATIVE
// DISABLE_NATIVE: isAppleTarget=false
// FILECHECK_STAGE: CStubs

// MODULE: cinterop
// FILE: direct.def
language = Objective-C
headers = direct.h
headerFilter = direct.h

// FILE: direct.h
#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

@interface CallingConventions : NSObject

+ (uint64_t)regular:(uint64_t)arg;
- (uint64_t)regular:(uint64_t)arg;

+ (uint64_t)direct:(uint64_t)arg __attribute__((objc_direct));
- (uint64_t)direct:(uint64_t)arg __attribute__((objc_direct));

@end

NS_ASSUME_NONNULL_END

// FILE: direct.m
#import "direct.h"

#define TEST_METHOD_IMPL(NAME) (uint64_t)NAME:(uint64_t)arg { return arg; }

@implementation CallingConventions : NSObject

+ TEST_METHOD_IMPL(regular);
- TEST_METHOD_IMPL(regular);

+ TEST_METHOD_IMPL(direct);
- TEST_METHOD_IMPL(direct);

@end



// MODULE: main(cinterop)
// FILE: main.kt
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
import kotlin.native.Retain
import direct.*

// KT-54610
fun box(): String {
    callDirect()
    callRegular()
    return "OK"
}

@Retain
//CHECK-LABEL: define i64 @"kfun:#callDirect(){}kotlin.ULong"()
fun callDirect(): ULong {
    val cc = CallingConventions()
    //CHECK: invoke i64 @_{{[a-zA-Z0-9]+}}_knbridge{{[0-9]+}}(i8* %{{[0-9]+}}, i64 42)
    return cc.direct(42uL)
}

@Retain
//CHECK-LABEL: define i64 @"kfun:#callRegular(){}kotlin.ULong"()
fun callRegular(): ULong {
    val cc = CallingConventions()
    //CHECK: invoke i64 @_{{[a-zA-Z0-9]+}}_knbridge{{[0-9]+}}(i8* %{{[0-9]+}}, i8* %{{[0-9]+}}, i64 42)
    return cc.regular(42uL)
}
