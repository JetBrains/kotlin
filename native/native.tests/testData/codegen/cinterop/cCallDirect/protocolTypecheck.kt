// TARGET_BACKEND: NATIVE
// FREE_CINTEROP_ARGS: -Xccall-mode direct
// MODULE: cinterop
// FILE: lib.def
headers = lib.h
language = Objective-C

// FILE: lib.h
@protocol Foo
@end

@protocol Foo2
@end

@protocol Foo3
@end

id getFooImpl(void);
id getNotFoo(void);

id sameObjC(id p);

// FILE: lib.m
#import "lib.h"
#import <Foundation/Foundation.h>

@interface FooImpl : NSObject <Foo>
@end

@implementation FooImpl
@end

@interface Foo2Impl : NSObject <Foo2>
@end

// It makes sure that `Foo2` exists in the compiled code.
@implementation Foo2Impl
@end
// While `Foo3` doesn't.

id getFooImpl(void) {
    return [FooImpl new];
}

id getNotFoo(void) {
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
    if (getFooImpl() !is FooProtocol) return "FAIL 1"
    // The check below also tests the case when `objc_getProtocol("Foo") != nullptr`
    // is cached in `getProtocolByName`:
    if (getFooImpl() !is FooProtocol) return "FAIL 2"
    if (getNotFoo() is FooProtocol) return "FAIL 3"

    if (getFooImpl() is Foo2Protocol) return "FAIL 4"
    if (getNotFoo() is Foo2Protocol) return "FAIL 5"

    if (getFooImpl() is Foo3Protocol) return "FAIL 6"
    if (getNotFoo() is Foo3Protocol) return "FAIL 7"

    if (same(Any()) is FooProtocol) return "FAIL 8"
    if (sameObjC(Any()) is FooProtocol) return "FAIL 9"

    return "OK"
}
