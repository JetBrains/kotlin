// TARGET_BACKEND: NATIVE
// FREE_CINTEROP_ARGS: -Xccall-mode direct
// MODULE: cinterop
// FILE: lib.def
headers = lib.h
language = Objective-C

// FILE: lib.h
@protocol Baz;

id getBazImpl(void);
id getNotBaz(void);

// FILE: lib.m
#import "lib.h"
#import <Foundation/Foundation.h>

@protocol Baz
@end

@interface BazImpl : NSObject <Baz>
@end

@implementation BazImpl
@end

id getBazImpl(void) {
    return [BazImpl new];
}

id getNotBaz(void) {
    return [NSObject new];
}

// MODULE: main(cinterop)
// FILE: main.kt
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

import lib.*
import objcnames.protocols.BazProtocol

fun box(): String {
    // Type checks against forward declarations are unchecked and always succeed.
    // So, all objects can be cast to `objcnames.protocols.BazProtocol`.
    // Similarly, `is`-checks aren't supported, so the test has to do `as`-checks.

    val bazImpl = getBazImpl()
    if (bazImpl as BazProtocol != bazImpl) return "FAIL 1"

    val notBaz = getNotBaz()
    if (notBaz as BazProtocol != notBaz) return "FAIL 2"

    return "OK"
}
