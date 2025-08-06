// TARGET_BACKEND: NATIVE
// FREE_CINTEROP_ARGS: -Xccall-mode direct
// MODULE: cinterop
// FILE: lib.def
headers = lib.h
language = Objective-C

// FILE: lib.h
@protocol Qux
@end

id getQuxImpl(void);
id getQuxImplClass(void);
id getNotQux(void);
id getNotQuxClass(void);

id sameObjC(id p);

// FILE: lib.m
#import "lib.h"
#import <Foundation/Foundation.h>

@interface QuxImpl : NSObject <Qux>
@end

@implementation QuxImpl
@end

id getQuxImpl(void) {
    return [QuxImpl new];
}

id getQuxImplClass(void) {
    return [QuxImpl class];
}

id getNotQux(void) {
    return [NSObject new];
}

id getNotQuxClass(void) {
    return [NSObject class];
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

fun box(): String {
    if (getQuxImplClass() !is QuxProtocolMeta) return "FAIL 1"
    if (getQuxImplClass() is QuxProtocol) return "FAIL 2"

    if (getQuxImpl() is QuxProtocolMeta) return "FAIL 3"
    if (getQuxImpl() !is QuxProtocol) return "FAIL 4"

    if (getNotQuxClass() is QuxProtocolMeta) return "FAIL 5"
    if (getNotQuxClass() is QuxProtocol) return "FAIL 6"

    if (getNotQux() is QuxProtocolMeta) return "FAIL 7"
    if (getNotQux() is QuxProtocol) return "FAIL 8"

    if (same(Any()) is QuxProtocolMeta) return "FAIL 9"
    if (same(Any()) is QuxProtocol) return "FAIL 10"
    if (sameObjC(Any()) is QuxProtocolMeta) return "FAIL 11"
    if (sameObjC(Any()) is QuxProtocol) return "FAIL 12"

    return "OK"
}
