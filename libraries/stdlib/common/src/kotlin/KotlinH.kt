/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin

import kotlin.annotation.AnnotationTarget.FIELD
import kotlin.annotation.AnnotationTarget.PROPERTY



expect interface Comparator<T> {
    fun compare(a: T, b: T): Int
}

// TODO: Satisfied with SAM-constructor for Comparator interface in JVM
@Suppress("NO_ACTUAL_FOR_EXPECT")
expect inline fun <T> Comparator(crossinline comparison: (a: T, b: T) -> Int): Comparator<T>

// From kotlin.kt



// From numbers.kt

expect fun Double.isNaN(): Boolean
expect fun Float.isNaN(): Boolean
expect fun Double.isInfinite(): Boolean
expect fun Float.isInfinite(): Boolean
expect fun Double.isFinite(): Boolean
expect fun Float.isFinite(): Boolean

/**
 * Returns a bit representation of the specified floating-point value as [Long]
 * according to the IEEE 754 floating-point "double format" bit layout.
 */
@SinceKotlin("1.2")
public expect fun Double.toBits(): Long

/**
 * Returns a bit representation of the specified floating-point value as [Long]
 * according to the IEEE 754 floating-point "double format" bit layout,
 * preserving `NaN` values exact layout.
 */
@SinceKotlin("1.2")
public expect fun Double.toRawBits(): Long

/**
 * Returns the [Double] value corresponding to a given bit representation.
 */
@SinceKotlin("1.2")
public expect fun Double.Companion.fromBits(bits: Long): Double

/**
 * Returns a bit representation of the specified floating-point value as [Int]
 * according to the IEEE 754 floating-point "single format" bit layout.
 */
@SinceKotlin("1.2")
public expect fun Float.toBits(): Int

/**
 * Returns a bit representation of the specified floating-point value as [Int]
 * according to the IEEE 754 floating-point "single format" bit layout,
 * preserving `NaN` values exact layout.
 */
@SinceKotlin("1.2")
public expect fun Float.toRawBits(): Int

/**
 * Returns the [Float] value corresponding to a given bit representation.
 */
@SinceKotlin("1.2")
public expect fun Float.Companion.fromBits(bits: Int): Float


// From concurrent.kt

// TODO: promote to error? Otherwise it gets to JVM part
//@Deprecated("Use Volatile annotation from kotlin.jvm package", ReplaceWith("kotlin.jvm.Volatile"), level = DeprecationLevel.WARNING)
//public typealias Volatile = kotlin.jvm.Volatile

@Deprecated("Synchronization on any object is not supported on every platform and will be removed from the common standard library soon.")
public expect inline fun <R> synchronized(lock: Any, block: () -> R): R




// from lazy.kt

public expect fun <T> lazy(initializer: () -> T): Lazy<T>

/**
 * Creates a new instance of the [Lazy] that uses the specified initialization function [initializer].
 *
 * The [mode] parameter is ignored. */
public expect fun <T> lazy(mode: LazyThreadSafetyMode, initializer: () -> T): Lazy<T>

/**
 * Creates a new instance of the [Lazy] that uses the specified initialization function [initializer].
 *
 * The [lock] parameter is ignored.
 */
public expect fun <T> lazy(lock: Any?, initializer: () -> T): Lazy<T>
