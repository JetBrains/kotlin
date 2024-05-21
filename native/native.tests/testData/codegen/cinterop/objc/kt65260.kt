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
    val fooClass: Any = A
    assertTrue(fooClass is A.Companion)
    val barClass: Any = B
    assertTrue(barClass is AMeta)

    val aNativeHeir: Any = ANativeHeir
    assertFailsWith<ClassCastException> { aNativeHeir as A.Companion }
    val bNativeHeir: Any = BNativeHeir
    assertFailsWith<ClassCastException> { bNativeHeir as A.Companion }
    val fooObjectClass: Any = A
    assertFailsWith<ClassCastException> { fooObjectClass as BMeta }
}


// MODULE: main(cinterop, lib)
// FILE: main.kt
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
import kotlinx.cinterop.*
import platform.Foundation.*
import platform.darwin.*
import platform.objc.*
import kotlin.test.*

open class FooK {
    companion object
}

class BarK: FooK() {
    companion object
}

fun testAnyCast() {
    assertFailsWith<ClassCastException> { Any() as NSObject.Companion }
    assertFailsWith<ClassCastException> { Any() as NSNumber.Companion }
}

fun testMetaClassTypeChecking() {
    assertFalse(Any() is NSObject.Companion)
    assertFalse(Any() is NSNumber.Companion)

    val nsObjectClass: Any = NSObject
    assertFalse(nsObjectClass is NSNumberMeta)

    val nsDataClass: Any = NSData
    assertTrue(nsDataClass is NSData.Companion)

    val nsNumberClass: Any = NSNumber
    assertFalse(nsNumberClass is NSObject.Companion)
}

fun testNonObjCObjectTypeChecking() {
    val foo: Any = FooK
    assertTrue(foo is FooK.Companion)

    val bar: Any = BarK
    assertFalse(bar is FooK.Companion)
}

@OptIn(kotlinx.cinterop.BetaInteropApi::class)
fun testObjCMetaClassTypeChecking() {
    val nsObjectClass: ObjCClass = NSObject
    val nsObjectMetaClass: ObjCClass = object_getClass(nsObjectClass)!!
    assertTrue(nsObjectClass is NSObject.Companion)
    assertFalse(nsObjectMetaClass is NSObject.Companion)
    assertFalse(nsObjectClass == nsObjectMetaClass)

    val nsObjectCompanion: ObjCClass = NSObject.Companion
    assertTrue(nsObjectCompanion is NSObject.Companion)
    assertTrue(nsObjectClass == nsObjectCompanion)
}

fun box(): String {
    testAnyCast()
    testMetaClassTypeChecking()
    testExternalObjCMetaClassCast()
    testNonObjCObjectTypeChecking()
    testObjCMetaClassTypeChecking()

    return "OK"
}