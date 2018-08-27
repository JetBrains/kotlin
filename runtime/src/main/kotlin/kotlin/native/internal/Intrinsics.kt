/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.native.internal

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.NativePointed
import kotlinx.cinterop.NativePtr

@Intrinsic @PublishedApi external internal fun areEqualByValue(first: Boolean, second: Boolean): Boolean
@Intrinsic @PublishedApi external internal fun areEqualByValue(first: Byte, second: Byte): Boolean
@Intrinsic @PublishedApi external internal fun areEqualByValue(first: Short, second: Short): Boolean
@Intrinsic @PublishedApi external internal fun areEqualByValue(first: Int, second: Int): Boolean
@Intrinsic @PublishedApi external internal fun areEqualByValue(first: Long, second: Long): Boolean
@Intrinsic @PublishedApi external internal fun areEqualByValue(first: NativePtr, second: NativePtr): Boolean

// Bitwise equality:
@Intrinsic @PublishedApi external internal fun areEqualByValue(first: Float, second: Float): Boolean
@Intrinsic @PublishedApi external internal fun areEqualByValue(first: Double, second: Double): Boolean

// IEEE754 equality:
@Intrinsic @PublishedApi external internal fun ieee754Equals(first: Float, second: Float): Boolean
@Intrinsic @PublishedApi external internal fun ieee754Equals(first: Double, second: Double): Boolean

// Reinterprets this value from T to R having the same binary representation (e.g. to unwrap inline class).
@Intrinsic @PublishedApi external internal fun <T, R> T.reinterpret(): R
