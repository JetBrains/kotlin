/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:JvmMultifileClass
@file:JvmName("AtomicArraysKt")
@file:OptIn(ExperimentalAtomicApi::class)

package kotlin.concurrent.atomics

/**
 * Casts the given [AtomicIntArray] instance to [java.util.concurrent.atomic.AtomicIntegerArray].
 */
@SinceKotlin("2.1")
@Suppress("UNCHECKED_CAST")
@ExperimentalAtomicApi
public fun AtomicIntArray.asJavaAtomicArray(): java.util.concurrent.atomic.AtomicIntegerArray = this as java.util.concurrent.atomic.AtomicIntegerArray

/**
 * Casts the given [java.util.concurrent.atomic.AtomicIntegerArray] instance to [AtomicIntArray].
 */
@SinceKotlin("2.1")
@Suppress("UNCHECKED_CAST")
@ExperimentalAtomicApi
public fun java.util.concurrent.atomic.AtomicIntegerArray.asKotlinAtomicArray(): AtomicIntArray = this as AtomicIntArray

/**
 * Casts the given [AtomicLongArray] instance to [java.util.concurrent.atomic.AtomicLongArray].
 */
@SinceKotlin("2.1")
@Suppress("UNCHECKED_CAST")
@ExperimentalAtomicApi
public fun AtomicLongArray.asJavaAtomicArray(): java.util.concurrent.atomic.AtomicLongArray = this as java.util.concurrent.atomic.AtomicLongArray

/**
 * Casts the given [java.util.concurrent.atomic.AtomicLongArray] instance to [AtomicLongArray].
 */
@SinceKotlin("2.1")
@Suppress("UNCHECKED_CAST")
@ExperimentalAtomicApi
public fun java.util.concurrent.atomic.AtomicLongArray.asKotlinAtomicArray(): AtomicLongArray = this as AtomicLongArray

/**
 * Casts the given [AtomicArray]<T> instance to [java.util.concurrent.atomic.AtomicReferenceArray]<T>.
 */
@SinceKotlin("2.1")
@Suppress("UNCHECKED_CAST")
@ExperimentalAtomicApi
public fun <T> AtomicArray<T>.asJavaAtomicArray(): java.util.concurrent.atomic.AtomicReferenceArray<T> = this as java.util.concurrent.atomic.AtomicReferenceArray<T>

/**
 * Casts the given [java.util.concurrent.atomic.AtomicReferenceArray]<T> instance to [AtomicArray]<T>.
 */
@SinceKotlin("2.1")
@Suppress("UNCHECKED_CAST")
@ExperimentalAtomicApi
public fun <T> java.util.concurrent.atomic.AtomicReferenceArray<T>.asKotlinAtomicArray(): AtomicArray<T> = this as AtomicArray<T>


/**
 * Returns a new [AtomicArray] of the given type initialized with specified elements.
 *
 * @sample samples.concurrent.atomics.AtomicArray.factory
 */
@SinceKotlin("2.2")
@ExperimentalAtomicApi
@Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
public actual inline fun <T> atomicArrayOf(vararg elements: T): AtomicArray<T> = AtomicArray(elements as Array<T>)

/**
 * Returns a new [AtomicIntArray] containing the specified [Int] numbers.
 *
 * @sample samples.concurrent.atomics.AtomicIntArray.factory
 */
@SinceKotlin("2.2")
@ExperimentalAtomicApi
@Suppress("NOTHING_TO_INLINE")
public actual inline fun atomicIntArrayOf(vararg elements: Int): AtomicIntArray = AtomicIntArray(elements)

/**
 * Returns a new [AtomicLongArray] containing the specified [Long] numbers.
 *
 * @sample samples.concurrent.atomics.AtomicLongArray.factory
 */
@SinceKotlin("2.2")
@ExperimentalAtomicApi
@Suppress("NOTHING_TO_INLINE")
public actual inline fun atomicLongArrayOf(vararg elements: Long): AtomicLongArray = AtomicLongArray(elements)
