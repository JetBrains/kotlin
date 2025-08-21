/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.js

import kotlin.internal.UsedFromCompilerGeneratedCode

// Creates IntRange for {Byte, Short, Int}.rangeTo(x: {Byte, Short, Int})
@UsedFromCompilerGeneratedCode
internal fun numberRangeToNumber(start: dynamic, endInclusive: dynamic) =
    IntRange(start, endInclusive)

// Create LongRange for {Byte, Short, Int}.rangeTo(x: Long)
@UsedFromCompilerGeneratedCode
internal fun numberRangeToLong(start: Number, endInclusive: Long) =
    LongRange(start.toLong(), endInclusive)

// Create LongRange for Long.rangeTo(x: {Byte, Short, Int})
@UsedFromCompilerGeneratedCode
internal fun longRangeToNumber(start: Long, endInclusive: Number) =
    LongRange(start, endInclusive.toLong())

// Create LongRange for Long.rangeTo(x: Long)
@UsedFromCompilerGeneratedCode
internal fun longRangeToLong(start: Long, endInclusive: Long) =
    LongRange(start, endInclusive)
