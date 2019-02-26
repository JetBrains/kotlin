/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.math

/**
 * Returns the smaller of two values.
 */
@SinceKotlin("1.3")
@ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun min(a: UInt, b: UInt): UInt {
    return minOf(a, b)
}

/**
 * Returns the smaller of two values.
 */
@SinceKotlin("1.3")
@ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun min(a: ULong, b: ULong): ULong {
    return minOf(a, b)
}

/**
 * Returns the greater of two values.
 */
@SinceKotlin("1.3")
@ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun max(a: UInt, b: UInt): UInt {
    return maxOf(a, b)
}

/**
 * Returns the greater of two values.
 */
@SinceKotlin("1.3")
@ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun max(a: ULong, b: ULong): ULong {
    return maxOf(a, b)
}