// TARGET_BACKEND: NATIVE
// KT-82200
// IGNORE_BACKEND: NATIVE
// FREE_CINTEROP_ARGS: -Xccall-mode direct
// MODULE: cinterop
// FILE: lib.def
headers = lib.h
language = Objective-C

// FILE: lib.h
__attribute__((objc_runtime_name("CustomRuntimeName")))
@protocol Bar
@end

id getBarImpl(void);
id getNotBar(void);

id sameObjC(id p);

// FILE: lib.m
#import "lib.h"
#import <Foundation/Foundation.h>

@interface BarImpl : NSObject <Bar>
@end

@implementation BarImpl
@end

id getBarImpl(void) {
    return [BarImpl new];
}

id getNotBar(void) {
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

fun box(): String {
    if (getBarImpl() !is BarProtocol) return "FAIL 1"
    if (getNotBar() is BarProtocol) return "FAIL 2"

    if (same(Any()) is BarProtocol) return "FAIL 3"
    if (sameObjC(Any()) is BarProtocol) return "FAIL 4"

    return "OK"
}
