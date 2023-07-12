/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.concurrent

import kotlin.native.internal.*
import kotlin.reflect.*
import kotlin.concurrent.*
import kotlin.native.concurrent.*

/**
 * Atomically gets the value of the [IntArray][this] element at the given [index].
 *
 * Provides sequential consistent ordering guarantees.
 *
 * NOTE: Ensure that the provided [index] does not exceed the size of the [array][this]. Exceeding the array size may result in undefined behavior.
 */
@TypedIntrinsic(IntrinsicType.ATOMIC_GET_ARRAY_ELEMENT)
internal external fun IntArray.atomicGet(index: Int): Int

/**
 * Atomically sets the value of the [IntArray][this] element at the given [index] to the [new value][newValue].
 *
 * Provides sequential consistent ordering guarantees.
 *
 * NOTE: Ensure that the provided [index] does not exceed the size of the [array][this]. Exceeding the array size may result in undefined behavior.
 */
@TypedIntrinsic(IntrinsicType.ATOMIC_SET_ARRAY_ELEMENT)
internal external fun IntArray.atomicSet(index: Int, newValue: Int)

/**
 * Atomically sets the value of the [IntArray][this] element at the given [index] to the [new value][newValue]
 * and returns the old value of the element.
 *
 * Provides sequential consistent ordering guarantees.
 *
 * NOTE: Ensure that the provided [index] does not exceed the size of the [array][this]. Exceeding the array size may result in undefined behavior.
 */
@TypedIntrinsic(IntrinsicType.GET_AND_SET_ARRAY_ELEMENT)
internal external fun IntArray.getAndSet(index: Int, newValue: Int): Int

/**
 * Atomically adds the [given value][delta] to the [IntArray][this] element at the given [index]
 * and returns the old value of the element.
 *
 * Provides sequential consistent ordering guarantees.
 *
 * NOTE: Ensure that the provided [index] does not exceed the size of the [array][this]. Exceeding the array size may result in undefined behavior.
 */
@TypedIntrinsic(IntrinsicType.GET_AND_ADD_ARRAY_ELEMENT)
internal external fun IntArray.getAndAdd(index: Int, delta: Int): Int

/**
 * Atomically sets the value of the [IntArray][this] element at the given [index] to the [new value][newValue]
 * if the current value equals the [expected value][expectedValue] and returns the old value of the element in any case.
 *
 * Provides sequential consistent ordering guarantees and never fails spuriously.
 *
 * NOTE: Ensure that the provided [index] does not exceed the size of the [array][this]. Exceeding the array size may result in undefined behavior.
 */
@TypedIntrinsic(IntrinsicType.COMPARE_AND_EXCHANGE_ARRAY_ELEMENT)
internal external fun IntArray.compareAndExchange(index: Int, expectedValue: Int, newValue: Int): Int

/**
 * Atomically sets the value of the [IntArray][this] element at the given [index] to the [new value][newValue]
 * if the current value equals the [expected value][expectedValue].
 * Returns true if the operation was successful and false only if the current value of the element was not equal to the expected value.
 *
 * Provides sequential consistent ordering guarantees and never fails spuriously.
 *
 * NOTE: Ensure that the provided [index] does not exceed the size of the [array][this]. Exceeding the array size may result in undefined behavior.
 */
@TypedIntrinsic(IntrinsicType.COMPARE_AND_SET_ARRAY_ELEMENT)
internal external fun IntArray.compareAndSet(index: Int, expectedValue: Int, newValue: Int): Boolean

/**
 * Atomically gets the value of the [LongArray][this] element at the given [index].
 *
 * Provides sequential consistent ordering guarantees.
 *
 * NOTE: Ensure that the provided [index] does not exceed the size of the [array][this]. Exceeding the array size may result in undefined behavior.
 */
@TypedIntrinsic(IntrinsicType.ATOMIC_GET_ARRAY_ELEMENT)
internal external fun LongArray.atomicGet(index: Int): Long

/**
 * Atomically sets the value of the [LongArray][this] element at the given [index] to the [new value][newValue].
 *
 * Provides sequential consistent ordering guarantees.
 *
 * NOTE: Ensure that the provided [index] does not exceed the size of the [array][this]. Exceeding the array size may result in undefined behavior.
 */
@TypedIntrinsic(IntrinsicType.ATOMIC_SET_ARRAY_ELEMENT)
internal external fun LongArray.atomicSet(index: Int, newValue: Long)

/**
 * Atomically sets the value of the [LongArray][this] element at the given [index] to the [new value][newValue]
 * and returns the old value of the element.
 *
 * Provides sequential consistent ordering guarantees.
 *
 * NOTE: Ensure that the provided [index] does not exceed the size of the [array][this]. Exceeding the array size may result in undefined behavior.
 */
@TypedIntrinsic(IntrinsicType.GET_AND_SET_ARRAY_ELEMENT)
internal external fun LongArray.getAndSet(index: Int, newValue: Long): Long

/**
 * Atomically adds the [given value][delta] to the [LongArray][this] element at the given [index]
 * and returns the old value of the element.
 *
 * Provides sequential consistent ordering guarantees.
 *
 * NOTE: Ensure that the provided [index] does not exceed the size of the [array][this]. Exceeding the array size may result in undefined behavior.
 */
@TypedIntrinsic(IntrinsicType.GET_AND_ADD_ARRAY_ELEMENT)
internal external fun LongArray.getAndAdd(index: Int, delta: Long): Long

/**
 * Atomically sets the value of the [LongArray][this] element at the given [index] to the [new value][newValue]
 * if the current value equals the [expected value][expectedValue] and returns the old value of the element in any case.
 *
 * Provides sequential consistent ordering guarantees and never fails spuriously.
 *
 * NOTE: Ensure that the provided [index] does not exceed the size of the [array][this]. Exceeding the array size may result in undefined behavior.
 */
@TypedIntrinsic(IntrinsicType.COMPARE_AND_EXCHANGE_ARRAY_ELEMENT)
internal external fun LongArray.compareAndExchange(index: Int, expectedValue: Long, newValue: Long): Long

/**
 * Atomically sets the value of the [LongArray][this] element at the given [index] to the [new value][newValue]
 * if the current value equals the [expected value][expectedValue].
 * Returns true if the operation was successful and false only if the current value of the element was not equal to the expected value.
 *
 * Provides sequential consistent ordering guarantees and never fails spuriously.
 *
 * NOTE: Ensure that the provided [index] does not exceed the size of the [array][this]. Exceeding the array size may result in undefined behavior.
 */
@TypedIntrinsic(IntrinsicType.COMPARE_AND_SET_ARRAY_ELEMENT)
internal external fun LongArray.compareAndSet(index: Int, expectedValue: Long, newValue: Long): Boolean

/**
 * Atomically gets the value of the [Array<T>][this] element at the given [index].
 *
 * Provides sequential consistent ordering guarantees.
 *
 * NOTE: Ensure that the provided [index] does not exceed the size of the [array][this]. Exceeding the array size may result in undefined behavior.
 */
@TypedIntrinsic(IntrinsicType.ATOMIC_GET_ARRAY_ELEMENT)
internal external fun <T> Array<T>.atomicGet(index: Int): T

/**
 * Atomically sets the value of the [Array<T>][this] element at the given [index] to the [new value][newValue].
 *
 * Provides sequential consistent ordering guarantees.
 *
 * NOTE: Ensure that the provided [index] does not exceed the size of the [array][this]. Exceeding the array size may result in undefined behavior.
 */
@TypedIntrinsic(IntrinsicType.ATOMIC_SET_ARRAY_ELEMENT)
internal external fun <T> Array<T>.atomicSet(index: Int, newValue: T)

/**
 * Atomically sets the value of the [Array<T>][this] element at the given [index] to the [new value][newValue]
 * and returns the old value of the element.
 *
 * Provides sequential consistent ordering guarantees.
 *
 * NOTE: Ensure that the provided [index] does not exceed the size of the [array][this]. Exceeding the array size may result in undefined behavior.
 */
@TypedIntrinsic(IntrinsicType.GET_AND_SET_ARRAY_ELEMENT)
internal external fun <T> Array<T>.getAndSet(index: Int, value: T): T

/**
 * Atomically sets the value of the [Array<T>][this] element at the given [index] to the [new value][newValue]
 * if the current value equals the [expected value][expectedValue] and returns the old value of the element in any case.
 *
 * Comparison of values is done by reference.
 *
 * Provides sequential consistent ordering guarantees and never fails spuriously.
 *
 * NOTE: Ensure that the provided [index] does not exceed the size of the [array][this]. Exceeding the array size may result in undefined behavior.
 */
@TypedIntrinsic(IntrinsicType.COMPARE_AND_EXCHANGE_ARRAY_ELEMENT)
internal external fun <T> Array<T>.compareAndExchange(index: Int, expectedValue: T, newValue: T): T

/**
 * Atomically sets the value of the [Array<T>][this] element at the given [index] to the [new value][newValue]
 * if the current value equals the [expected value][expectedValue].
 * Returns true if the operation was successful and false only if the current value of the element was not equal to the expected value.
 *
 * Comparison of values is done by reference.
 *
 * Provides sequential consistent ordering guarantees and never fails spuriously.
 *
 * NOTE: Ensure that the provided [index] does not exceed the size of the [array][this]. Exceeding the array size may result in undefined behavior.
 */
@TypedIntrinsic(IntrinsicType.COMPARE_AND_SET_ARRAY_ELEMENT)
internal external fun <T> Array<T>.compareAndSet(index: Int, expectedValue: T, newValue: T): Boolean
