/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
// TARGET_BACKEND: NATIVE
// DISABLE_NATIVE: isAppleTarget=false
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
import platform.Foundation.*
import platform.darwin.*
import kotlinx.cinterop.internal.*
import kotlin.test.*

fun test1() {
    try {
        Any() as NSObject.Companion
    } catch (e: Exception) {
        assertTrue(e is ClassCastException)
    }
}

fun test2() {
    try {
        Any() as NSNumber.Companion
    } catch (e: Exception) {
        assertTrue(e is ClassCastException)
    }
}

fun test3() {
    assertFalse(Any() is NSObject.Companion)
    assertFalse(Any() is NSNumber.Companion)

    val nsObjectClass: Any = NSObject
    assertFalse(nsObjectClass is NSNumberMeta)
}

fun box(): String {
    test1()
    test2()
    test3()

    return "OK"
}