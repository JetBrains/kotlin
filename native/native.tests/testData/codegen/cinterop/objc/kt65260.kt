import java.lang.Exception

/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
// TARGET_BACKEND: NATIVE
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
    try {
        BNativeHeir as A.Companion
    } catch (e: Exception) {
        assertTrue(e is ClassCastException)
    }

    try {
        ANativeHeir as A.Companion
    } catch (e: Exception) {
        assertTrue(e is ClassCastException)
    }

    assertTrue(A is A.Companion)

    val fooObjectClass: Any = A
    val barObjectClass: Any = B
    assertFalse(fooObjectClass is BMeta)
    assertTrue(barObjectClass is AMeta)
}


// MODULE: main(cinterop, lib)
// FILE: main.kt
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
import platform.Foundation.*
import platform.darwin.*
import kotlin.test.*

fun testAnyCast() {
    try {
        Any() as NSObject.Companion
    } catch (e: Exception) {
        assertTrue(e is ClassCastException)
    }

    try {
        Any() as NSNumber.Companion
    } catch (e: Exception) {
        assertTrue(e is ClassCastException)
    }
}

fun testMetaClassCast() {
    assertFalse(Any() is NSObject.Companion)
    assertFalse(Any() is NSNumber.Companion)

    val nsObjectClass: Any = NSObject
    assertFalse(nsObjectClass is NSNumberMeta)

    try {
        NSNumber as NSObject.Companion
    } catch (e: Exception) {
        assertTrue(e is ClassCastException)
    }
    assertTrue(NSData is NSData.Companion)
}

fun box(): String {
    testAnyCast()
    testMetaClassCast()
    testExternalObjCMetaClassCast()

    return "OK"
}