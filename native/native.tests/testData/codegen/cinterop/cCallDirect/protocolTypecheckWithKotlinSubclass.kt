// TARGET_BACKEND: NATIVE
// FREE_CINTEROP_ARGS: -Xccall-mode direct
// MODULE: cinterop
// FILE: lib.def
headers = lib.h
headerFilter = lib.h
language = Objective-C

// FILE: lib.h
#import <Foundation/Foundation.h>

@protocol KotlinProtocol
@end

@protocol KotlinProtocol2
@end

@protocol KotlinProtocol3
@end

@interface KotlinBaseClass : NSObject
@end

id getNotKotlinProtocol(void);

id sameObjC(id p);

// FILE: lib.m
#import "lib.h"
#import <Foundation/Foundation.h>

@implementation KotlinBaseClass
@end

@interface KotlinProtocol3Impl : NSObject <KotlinProtocol3>
@end
// It makes sure that `KotlinProtocol3` exists in the compiled code.
@implementation KotlinProtocol3Impl
@end
// While `KotlinProtocol` and `KotlinProtocol2` don't.

id getNotKotlinProtocol(void) {
    return [NSObject new];
}

// Trick the Kotlin compiler into generating an actual runtime type check
// by making it unable to optimize it out.
id sameObjC(id p) {
    return p;
}

// MODULE: main(cinterop)
// FILE: main.kt
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

import lib.*

private lateinit var global: Any

// Trick the Kotlin compiler into generating an actual runtime type check
// by making it unable to optimize it out.
fun same(obj: Any): Any {
    global = obj
    return global
}

class KotlinClass : KotlinBaseClass(), KotlinProtocolProtocol, KotlinProtocol3Protocol {
    companion object : KotlinBaseClassMeta(), KotlinProtocolProtocolMeta
}

fun box(): String {
    if (same(KotlinClass()) !is KotlinProtocolProtocol) return "FAIL 1"
    // The check below also tests the case when `objc_getProtocol("KotlinProtocol") == nullptr`
    // is cached in `getProtocolByName`:
    if (sameObjC(KotlinClass()) !is KotlinProtocolProtocol) return "FAIL 2"

    if (same(KotlinClass()) is KotlinProtocol2Protocol) return "FAIL 3"
    if (sameObjC(KotlinClass()) is KotlinProtocol2Protocol) return "FAIL 4"

    if (same(KotlinClass()) !is KotlinProtocol3Protocol) return "FAIL 5"

    if (getNotKotlinProtocol() is KotlinProtocolProtocol) return "FAIL 6"

    if (same(Any()) is KotlinProtocolProtocol) return "FAIL 7"
    if (sameObjC(Any()) is KotlinProtocolProtocol) return "FAIL 8"

    if (same(KotlinClass) !is KotlinProtocolProtocolMeta) return "FAIL 9"
    if (same(KotlinClass) is KotlinProtocolProtocol) return "FAIL 10"

    if (same(KotlinClass()) is KotlinProtocolProtocolMeta) return "FAIL 11"

    if (same(KotlinClass) !is KotlinProtocol3ProtocolMeta) return "FAIL 12"
    if (same(KotlinClass) is KotlinProtocol3Protocol) return "FAIL 13"

    if (same(KotlinClass()) is KotlinProtocol3ProtocolMeta) return "FAIL 14"

    // Check protocol adopted by a base class.
    if (same(KotlinClass()) !is NSObjectProtocol) return "FAIL 15"
    if (same(KotlinClass()) is NSObjectProtocolMeta) return "FAIL 16"
    if (same(KotlinClass) is NSObjectProtocol) return "FAIL 17"
    if (same(KotlinClass) !is NSObjectProtocolMeta) return "FAIL 18"

    return "OK"
}
