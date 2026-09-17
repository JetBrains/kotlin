// TARGET_BACKEND: NATIVE
// DISABLE_NATIVE: isAppleTarget=false
// WITH_PLATFORM_LIBS

// MODULE: cinterop
// FILE: lib.def
language = Objective-C
headers = lib.h
headerFilter = lib.h

// FILE: lib.h
#import <Foundation/NSObject.h>
#import <Foundation/NSString.h>

@interface Base : NSObject
- (id)objectFor:(id)key;
@end

static NSString* describe(id object) {
    return [object description];
}

static id callObjectFor(Base* base, id key) {
    return [base objectFor:key];
}

// FILE: lib.m
#import "lib.h"

@implementation Base
- (id)objectFor:(id)key {
    return nil;
}
@end

// MODULE: main(cinterop)
// FILE: main.kt
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

import lib.*
import platform.darwin.NSObject

class Described : NSObject() {
    override fun description(): String = "Described"
}

class Derived : Base() {
    override fun objectFor(key: Any?): Any? = listOf(key, "value")
}

fun box(): String {
    describe(Described()).let { if (it != "Described") return "FAIL description: $it" }
    callObjectFor(Derived(), "key").let { if (it != listOf("key", "value")) return "FAIL objectFor: $it" }
    return "OK"
}
