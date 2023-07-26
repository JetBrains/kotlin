/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
@file:OptIn(ExperimentalForeignApi::class)

package kotlin.native.internal

import kotlinx.cinterop.*

import kotlin.native.internal.TypedIntrinsic
import kotlin.native.internal.IntrinsicType

@TypedIntrinsic(IntrinsicType.ARE_EQUAL_BY_VALUE) @PublishedApi external internal fun areEqualByValue(first: Boolean, second: Boolean): Boolean
@TypedIntrinsic(IntrinsicType.ARE_EQUAL_BY_VALUE) @PublishedApi external internal fun areEqualByValue(first: Byte, second: Byte): Boolean
@TypedIntrinsic(IntrinsicType.ARE_EQUAL_BY_VALUE) @PublishedApi external internal fun areEqualByValue(first: Short, second: Short): Boolean
@TypedIntrinsic(IntrinsicType.ARE_EQUAL_BY_VALUE) @PublishedApi external internal fun areEqualByValue(first: Int, second: Int): Boolean
@TypedIntrinsic(IntrinsicType.ARE_EQUAL_BY_VALUE) @PublishedApi external internal fun areEqualByValue(first: Long, second: Long): Boolean
@TypedIntrinsic(IntrinsicType.ARE_EQUAL_BY_VALUE) @PublishedApi external internal fun areEqualByValue(first: NativePtr, second: NativePtr): Boolean

// Bitwise equality:
@TypedIntrinsic(IntrinsicType.ARE_EQUAL_BY_VALUE) @PublishedApi external internal fun areEqualByValue(first: Float, second: Float): Boolean
@TypedIntrinsic(IntrinsicType.ARE_EQUAL_BY_VALUE) @PublishedApi external internal fun areEqualByValue(first: Double, second: Double): Boolean
@TypedIntrinsic(IntrinsicType.ARE_EQUAL_BY_VALUE) @PublishedApi external internal fun areEqualByValue(first: Vector128, second: Vector128): Boolean

// IEEE754 equality:
@TypedIntrinsic(IntrinsicType.IEEE_754_EQUALS) @PublishedApi external internal fun ieee754Equals(first: Float, second: Float): Boolean
@TypedIntrinsic(IntrinsicType.IEEE_754_EQUALS) @PublishedApi external internal fun ieee754Equals(first: Double, second: Double): Boolean

// Reinterprets this value from T to R having the same binary representation (e.g. to unwrap inline class).
@TypedIntrinsic(IntrinsicType.IDENTITY) @PublishedApi external internal fun <T, R> T.reinterpret(): R


@TypedIntrinsic(IntrinsicType.THE_UNIT_INSTANCE) @ExportForCompiler external internal fun theUnitInstance(): Unit
