/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin


// From numbers.kt

/**
 * Returns `true` if the specified number is a
 * Not-a-Number (NaN) value, `false` otherwise.
 */
public expect fun Double.isNaN(): Boolean

/**
 * Returns `true` if the specified number is a
 * Not-a-Number (NaN) value, `false` otherwise.
 */
public expect fun Float.isNaN(): Boolean

/**
 * Returns `true` if this value is infinitely large in magnitude.
 */
public expect fun Double.isInfinite(): Boolean

/**
 * Returns `true` if this value is infinitely large in magnitude.
 */
public expect fun Float.isInfinite(): Boolean

/**
 * Returns `true` if the argument is a finite floating-point value; returns `false` otherwise (for `NaN` and infinity arguments).
 */
public expect fun Double.isFinite(): Boolean

/**
 * Returns `true` if the argument is a finite floating-point value; returns `false` otherwise (for `NaN` and infinity arguments).
 */
public expect fun Float.isFinite(): Boolean

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


/**
 * Creates a new instance of the [Lazy] that uses the specified initialization function [initializer]
 * and the default thread-safety mode [LazyThreadSafetyMode.SYNCHRONIZED].
 * The lock used is both platform- and implementation- specific detail.
 *
 * If the initialization of a value throws an exception, it will attempt to reinitialize the value at next access.
 *
 * @sample samples.lazy.LazySamples.lazySample
 */
public expect fun <T> lazy(initializer: () -> T): Lazy<T>

/**
 * Creates a new instance of the [Lazy] that uses the specified initialization function [initializer]
 * and thread-safety [mode].
 *
 * If the initialization of a value throws an exception, it will attempt to reinitialize the value at next access.
 *
 * For [LazyThreadSafetyMode.SYNCHRONIZED], the lock used is both platform- and implementation- specific detail.
 *
 * @sample samples.lazy.LazySamples.lazySynchronizedSample
 * @sample samples.lazy.LazySamples.lazySafePublicationSample
 */
public expect fun <T> lazy(mode: LazyThreadSafetyMode, initializer: () -> T): Lazy<T>

/**
 * Creates a new instance of the [Lazy] that uses the specified initialization function [initializer].
 *
 * The [lock] parameter is ignored.
 */
@Deprecated("Synchronization on Any? object is supported only in Kotlin/JVM.", ReplaceWith("lazy(initializer)"))
@DeprecatedSinceKotlin(warningSince = "1.9", errorSince = "2.1")
public expect fun <T> lazy(lock: Any?, initializer: () -> T): Lazy<T>
