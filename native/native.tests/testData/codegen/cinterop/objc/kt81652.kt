/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
// TARGET_BACKEND: NATIVE
// DISABLE_NATIVE: isAppleTarget=false
// WITH_PLATFORM_LIBS

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

NS_ASSUME_NONNULL_END

// FILE: conversion.m
#import "conversion.h"

@implementation A
@end

// MODULE: main(cinterop)
// FILE: main.kt
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
import conversion.*
import platform.darwin.*
import kotlinx.cinterop.*
import kotlin.test.*

class ANativeHeir : A() {
    companion object
}

fun box(): String {
    val fooClass: Any = A
    assertTrue(fooClass is A.Companion)

    val aNativeHeir: Any = ANativeHeir
    assertFailsWith<ClassCastException> { aNativeHeir as A.Companion }

    return "OK"
}