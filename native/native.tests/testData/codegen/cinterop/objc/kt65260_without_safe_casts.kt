/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=genericSafeCasts=false
// DISABLE_NATIVE: isAppleTarget=false

// MODULE: cinterop
// FILE: conversion.def
language = Objective-C
headers = conversion.h
headerFilter = conversion.h

// FILE: conversion.h
#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

__attribute__((objc_runtime_name("Foo")))
@interface A : NSObject
@end

__attribute__((objc_runtime_name("Bar")))
@interface B : A
@end

NS_ASSUME_NONNULL_END

// FILE: conversion.m
#import "conversion.h"

@implementation A
@end

@implementation B
@end

// MODULE: lib(cinterop)
// FILE: lib.kt
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
import conversion.*
import platform.darwin.*
import kotlinx.cinterop.*
import kotlin.test.*

class ANativeHeir : A() {
    companion object
}

class BNativeHeir : B() {
    companion object
}

fun testExternalObjCMetaClassCast() {
    val aNativeHeir: Any = ANativeHeir
    assertFailsWith<ClassCastException> { aNativeHeir as A.Companion }
    val bNativeHeir: Any = BNativeHeir
    assertFailsWith<ClassCastException> { bNativeHeir as A.Companion }
}


// MODULE: main(cinterop, lib)
// FILE: main.kt

fun box(): String {
    testExternalObjCMetaClassCast()

    return "OK"
}