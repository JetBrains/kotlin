/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin

import kotlin.annotation.AnnotationTarget.FIELD
import kotlin.annotation.AnnotationTarget.PROPERTY
import kotlin.wasm.internal.wasm_f32_reinterpret_i32
import kotlin.wasm.internal.wasm_f64_reinterpret_i64
import kotlin.wasm.internal.wasm_i32_reinterpret_f32
import kotlin.wasm.internal.wasm_i64_reinterpret_f64


/**
 * Provides a comparison function for imposing a total ordering between instances of the type [T].
 */
public actual fun interface Comparator<T> {
    /**
     * Compares its two arguments for order. Returns zero if the arguments are equal,
     * a negative number if the first argument is less than the second, or a positive number
     * if the first argument is greater than the second.
     */
    public actual fun compare(a: T, b: T): Int
}

// From numbers.kt

actual fun Double.isNaN(): Boolean = this != this
actual fun Float.isNaN(): Boolean = this != this
actual fun Double.isInfinite(): Boolean = (this == Double.POSITIVE_INFINITY) || (this == Double.NEGATIVE_INFINITY)
actual fun Float.isInfinite(): Boolean = (this == Float.POSITIVE_INFINITY) || (this == Float.NEGATIVE_INFINITY)
actual fun Double.isFinite(): Boolean = !isInfinite() && !isNaN()
actual fun Float.isFinite(): Boolean = !isInfinite() && !isNaN()

/**
 * Returns a bit representation of the specified floating-point value as [Long]
 * according to the IEEE 754 floating-point "double format" bit layout.
 */
@SinceKotlin("1.2")
public actual fun Double.toBits(): Long = if (isNaN()) Double.NaN.toRawBits() else toRawBits()

/**
 * Returns a bit representation of the specified floating-point value as [Long]
 * according to the IEEE 754 floating-point "double format" bit layout,
 * preserving `NaN` values exact layout.
 */
@SinceKotlin("1.2")
public actual fun Double.toRawBits(): Long = wasm_i64_reinterpret_f64(this)

/**
 * Returns the [Double] value corresponding to a given bit representation.
 */
@SinceKotlin("1.2")
public actual fun Double.Companion.fromBits(bits: Long): Double = wasm_f64_reinterpret_i64(bits)

/**
 * Returns a bit representation of the specified floating-point value as [Int]
 * according to the IEEE 754 floating-point "single format" bit layout.
 */
@SinceKotlin("1.2")
public actual fun Float.toBits(): Int = if (isNaN()) Float.NaN.toRawBits() else toRawBits()

/**
 * Returns a bit representation of the specified floating-point value as [Int]
 * according to the IEEE 754 floating-point "single format" bit layout,
 * preserving `NaN` values exact layout.
 */
@SinceKotlin("1.2")
public actual fun Float.toRawBits(): Int = wasm_i32_reinterpret_f32(this)

/**
 * Returns the [Float] value corresponding to a given bit representation.
 */
@SinceKotlin("1.2")
public actual fun Float.Companion.fromBits(bits: Int): Float = wasm_f32_reinterpret_i32(bits)


// From concurrent.kt

// TODO: promote to error? Otherwise it gets to JVM part
//@Deprecated("Use Volatile annotation from kotlin.jvm package", ReplaceWith("kotlin.jvm.Volatile"), level = DeprecationLevel.WARNING)
//public typealias Volatile = kotlin.jvm.Volatile

// from lazy.kt

/**
 * Creates a new instance of the [Lazy] that uses the specified initialization function [initializer].
 */
public actual fun <T> lazy(initializer: () -> T): Lazy<T> = UnsafeLazyImpl(initializer)

/**
 * Creates a new instance of the [Lazy] that uses the specified initialization function [initializer].
 *
 * The [mode] parameter is ignored. */
public actual fun <T> lazy(mode: LazyThreadSafetyMode, initializer: () -> T): Lazy<T> = UnsafeLazyImpl(initializer)

/**
 * Creates a new instance of the [Lazy] that uses the specified initialization function [initializer].
 *
 * The [lock] parameter is ignored.
 */
public actual fun <T> lazy(lock: Any?, initializer: () -> T): Lazy<T> = UnsafeLazyImpl(initializer)
