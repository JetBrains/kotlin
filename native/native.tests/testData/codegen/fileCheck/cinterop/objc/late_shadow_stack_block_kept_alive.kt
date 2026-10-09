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

typedef int (^IntBlock)(int);
IntBlock getBlock(void);

// FILE: objclib.m
#import "objclib.h"

IntBlock getBlock(void) { return ^int(int x) { return x + 1; }; }

// MODULE: main(objclib)
// FILE: main.kt
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

import objclib.getBlock

// CHECK-LABEL: define internal i32 @"kfun:{{.*}}BlockFunctionImpl{{[0-9]+}}.invoke#internal"
// CHECK: call ptr @Kotlin_Interop_refToObjC
// CHECK: call_success:
// CHECK: call void @Kotlin_gc_keepAlive(ptr addrspace(1)
// CHECK-LABEL: epilogue:
fun box(): String = if (getBlock()!!(41) == 42) "OK" else "FAIL"
