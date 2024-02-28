/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
// TARGET_BACKEND: NATIVE
// DISABLE_NATIVE: isAppleTarget=false
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
import platform.Foundation.*
import platform.darwin.*
import kotlin.test.*

fun box(): String {
    try {
        // Test: cast to NSObject Metaclass
        Any() as NSObject.Companion
        // Test: cast to subclass of NSObject Metaclass
        Any() as NSString.Companion
        Any() as NSArray.Companion
    } catch (e: Exception) {
        // Cannot access 'TypeCastException': it is internal in Kotlin/Native.
        assertTrue(e is ClassCastException)
    }
    return "OK"
}