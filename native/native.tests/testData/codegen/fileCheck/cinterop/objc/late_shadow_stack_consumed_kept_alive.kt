// TARGET_BACKEND: NATIVE
// DISABLE_NATIVE: isAppleTarget=false
// FILECHECK_STAGE: CStubs
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true

// MODULE: objclib
// FILE: objclib.def
language = Objective-C
headers = objclib.h
headerFilter = objclib.h

// FILE: objclib.h
#import <Foundation/Foundation.h>

@interface Consumer : NSObject
- (void)take:(id) __attribute__((ns_consumed)) obj;
- (void)consumeSelf __attribute__((ns_consumes_self));
@end

// FILE: objclib.m
#import "objclib.h"

@implementation Consumer
- (void)take:(id) __attribute__((ns_consumed)) obj {}
- (void)consumeSelf __attribute__((ns_consumes_self)) {}
@end

// MODULE: main(objclib)
// FILE: main.kt
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

import objclib.Consumer

// CHECK-LABEL: define void @"kfun:{{.*}}passConsumed
// CHECK: call ptr @Kotlin_objc_retain_inNative
// CHECK: call_success:
// CHECK: call void @Kotlin_gc_keepAlive(ptr addrspace(1)
// CHECK: call void @Kotlin_gc_keepAlive(ptr addrspace(1)
// CHECK-LABEL: epilogue:
fun passConsumed(consumer: Consumer, obj: Consumer) = consumer.take(obj)

// CHECK-LABEL: define void @"kfun:{{.*}}consumeReceiver
// CHECK: call ptr @Kotlin_objc_retain_inNative
// CHECK: call_success:
// CHECK: call void @Kotlin_gc_keepAlive(ptr addrspace(1)
// CHECK-LABEL: epilogue:
fun consumeReceiver(consumer: Consumer) = consumer.consumeSelf()

fun box(): String {
    passConsumed(Consumer(), Consumer())
    consumeReceiver(Consumer())
    return "OK"
}
