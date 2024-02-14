// TARGET_BACKEND: NATIVE
// DISABLE_NATIVE: isAppleTarget=false
// LANGUAGE: +ImplicitSignedToUnsignedIntegerConversion

// MODULE: cinterop
// FILE: direct.def
language = Objective-C
headers = direct.h
headerFilter = direct.h

// FILE: direct.h
#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

__attribute__((objc_runtime_name("CC")))
__attribute__((swift_name("CC")))
@interface CallingConventions : NSObject

+ (uint64_t)regular:(uint64_t)arg;
- (uint64_t)regular:(uint64_t)arg;

+ (uint64_t)direct:(uint64_t)arg __attribute__((objc_direct));
- (uint64_t)direct:(uint64_t)arg __attribute__((objc_direct));

@end

@interface CallingConventions(Ext)

+ (uint64_t)regularExt:(uint64_t)arg;
- (uint64_t)regularExt:(uint64_t)arg;

+ (uint64_t)directExt:(uint64_t)arg __attribute__((objc_direct));
- (uint64_t)directExt:(uint64_t)arg __attribute__((objc_direct));

@end

__attribute__((objc_runtime_name("CCH")))
__attribute__((swift_name("CCH")))
@interface CallingConventionsHeir : CallingConventions
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

@implementation CallingConventions(Ext)

+ TEST_METHOD_IMPL(regularExt);
- TEST_METHOD_IMPL(regularExt);

+ TEST_METHOD_IMPL(directExt);
- TEST_METHOD_IMPL(directExt);

@end

@implementation CallingConventionsHeir

@end


// MODULE: main(cinterop)
// FILE: main.kt
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
import direct.*
import kotlinx.cinterop.*
import kotlin.test.*

class CallingConventionsNativeHeir() : CallingConventions() {
    // nothing
}

typealias CC = CallingConventions
typealias CCH = CallingConventionsHeir
typealias CCN = CallingConventionsNativeHeir

// KT-54610
fun box(): String {
    autoreleasepool {
        val cc = CC()
        val cch = CCH()
        val ccn = CCN()

        assertEquals(42UL, CC.regular(42))
        assertEquals(42UL, cc.regular(42))
        assertEquals(42UL, CC.regularExt(42))
        assertEquals(42UL, cc.regularExt(42))

        assertEquals(42UL, CCH.regular(42))
        assertEquals(42UL, cch.regular(42))
        assertEquals(42UL, CCH.regularExt(42))
        assertEquals(42UL, cch.regularExt(42))

        assertEquals(42UL, ccn.regular(42UL))
        assertEquals(42UL, ccn.regularExt(42UL))

        assertEquals(42UL, CC.direct(42))
        assertEquals(42UL, cc.direct(42))
        assertEquals(42UL, CC.directExt(42))
        assertEquals(42UL, cc.directExt(42))

        assertEquals(42UL, CCH.direct(42))
        assertEquals(42UL, cch .direct(42))
        assertEquals(42UL, CCH.directExt(42))
        assertEquals(42UL, cch .directExt(42))

        assertEquals(42UL, ccn .direct(42UL))
        assertEquals(42UL, ccn .directExt(42UL))
    }

    return "OK"
}
