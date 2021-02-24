/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin

import kotlin.annotation.AnnotationTarget.FIELD
import kotlin.annotation.AnnotationTarget.PROPERTY


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

// From kotlin.kt



// From numbers.kt

actual fun Double.isNaN(): Boolean = TODO("Wasm stdlib: Kotlin")
actual fun Float.isNaN(): Boolean = TODO("Wasm stdlib: Kotlin")
actual fun Double.isInfinite(): Boolean = TODO("Wasm stdlib: Kotlin")
actual fun Float.isInfinite(): Boolean = TODO("Wasm stdlib: Kotlin")
actual fun Double.isFinite(): Boolean = TODO("Wasm stdlib: Kotlin")
actual fun Float.isFinite(): Boolean = TODO("Wasm stdlib: Kotlin")

/**
 * Returns a bit representation of the specified floating-point value as [Long]
 * according to the IEEE 754 floating-point "double format" bit layout.
 */
@SinceKotlin("1.2")
public actual fun Double.toBits(): Long = TODO("Wasm stdlib: Kotlin")

/**
 * Returns a bit representation of the specified floating-point value as [Long]
 * according to the IEEE 754 floating-point "double format" bit layout,
 * preserving `NaN` values exact layout.
 */
@SinceKotlin("1.2")
public actual fun Double.toRawBits(): Long = TODO("Wasm stdlib: Kotlin")

/**
 * Returns the [Double] value corresponding to a given bit representation.
 */
@SinceKotlin("1.2")
public actual fun Double.Companion.fromBits(bits: Long): Double = TODO("Wasm stdlib: Kotlin")

/**
 * Returns a bit representation of the specified floating-point value as [Int]
 * according to the IEEE 754 floating-point "single format" bit layout.
 */
@SinceKotlin("1.2")
public actual fun Float.toBits(): Int = TODO("Wasm stdlib: Kotlin")

/**
 * Returns a bit representation of the specified floating-point value as [Int]
 * according to the IEEE 754 floating-point "single format" bit layout,
 * preserving `NaN` values exact layout.
 */
@SinceKotlin("1.2")
public actual fun Float.toRawBits(): Int = TODO("Wasm stdlib: Kotlin")

/**
 * Returns the [Float] value corresponding to a given bit representation.
 */
@SinceKotlin("1.2")
public actual fun Float.Companion.fromBits(bits: Int): Float = TODO("Wasm stdlib: Kotlin")


// From concurrent.kt

// TODO: promote to error? Otherwise it gets to JVM part
//@Deprecated("Use Volatile annotation from kotlin.jvm package", ReplaceWith("kotlin.jvm.Volatile"), level = DeprecationLevel.WARNING)
//public typealias Volatile = kotlin.jvm.Volatile

@Deprecated("Synchronization on any object is not supported on every platform and will be removed from the common standard library soon.")
public actual inline fun <R> synchronized(lock: Any, block: () -> R): R = TODO("Wasm stdlib: Kotlin")




// from lazy.kt

public actual fun <T> lazy(initializer: () -> T): Lazy<T> = TODO("Wasm stdlib: Kotlin")

/**
 * Creates a new instance of the [Lazy] that uses the specified initialization function [initializer].
 *
 * The [mode] parameter is ignored. */
public actual fun <T> lazy(mode: LazyThreadSafetyMode, initializer: () -> T): Lazy<T> = TODO("Wasm stdlib: Kotlin")

/**
 * Creates a new instance of the [Lazy] that uses the specified initialization function [initializer].
 *
 * The [lock] parameter is ignored.
 */
public actual fun <T> lazy(lock: Any?, initializer: () -> T): Lazy<T> = TODO("Wasm stdlib: Kotlin")
