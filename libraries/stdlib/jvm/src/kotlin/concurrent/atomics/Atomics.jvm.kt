/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:JvmMultifileClass
@file:JvmName("AtomicsKt")
@file:OptIn(ExperimentalAtomicApi::class)

package kotlin.concurrent.atomics

/**
 * Casts the given [AtomicInt] instance to [java.util.concurrent.atomic.AtomicInteger].
 */
@SinceKotlin("2.1")
@Suppress("UNCHECKED_CAST")
@ExperimentalAtomicApi
public fun AtomicInt.asJavaAtomic(): java.util.concurrent.atomic.AtomicInteger = this as java.util.concurrent.atomic.AtomicInteger

/**
 * Casts the given [java.util.concurrent.atomic.AtomicInteger] instance to [AtomicInt].
 */
@SinceKotlin("2.1")
@Suppress("UNCHECKED_CAST")
@ExperimentalAtomicApi
public fun java.util.concurrent.atomic.AtomicInteger.asKotlinAtomic(): AtomicInt = this as AtomicInt

/**
 * Casts the given [AtomicLong] instance to [java.util.concurrent.atomic.AtomicLong].
 */
@SinceKotlin("2.1")
@Suppress("UNCHECKED_CAST")
@ExperimentalAtomicApi
public fun AtomicLong.asJavaAtomic(): java.util.concurrent.atomic.AtomicLong = this as java.util.concurrent.atomic.AtomicLong

/**
 * Casts the given [java.util.concurrent.atomic.AtomicLong] instance to [AtomicLong].
 */
@SinceKotlin("2.1")
@Suppress("UNCHECKED_CAST")
@ExperimentalAtomicApi
public fun java.util.concurrent.atomic.AtomicLong.asKotlinAtomic(): AtomicLong = this as AtomicLong

/**
 * Casts the given [AtomicBoolean] instance to [java.util.concurrent.atomic.AtomicBoolean].
 */
@SinceKotlin("2.1")
@Suppress("UNCHECKED_CAST")
@ExperimentalAtomicApi
public fun <T> AtomicBoolean.asJavaAtomic(): java.util.concurrent.atomic.AtomicBoolean = this as java.util.concurrent.atomic.AtomicBoolean

/**
 * Casts the given [java.util.concurrent.atomic.AtomicBoolean] instance to [AtomicBoolean].
 */
@SinceKotlin("2.1")
@Suppress("UNCHECKED_CAST")
@ExperimentalAtomicApi
public fun <T> java.util.concurrent.atomic.AtomicBoolean.asKotlinAtomic(): AtomicBoolean = this as AtomicBoolean

/**
 * Casts the given [AtomicReference]<T> instance to [java.util.concurrent.atomic.AtomicReference]<T>.
 */
@SinceKotlin("2.1")
@Suppress("UNCHECKED_CAST")
@ExperimentalAtomicApi
public fun <T> AtomicReference<T>.asJavaAtomic(): java.util.concurrent.atomic.AtomicReference<T> = this as java.util.concurrent.atomic.AtomicReference<T>

/**
 * Casts the given [java.util.concurrent.atomic.AtomicReference]<T> instance to [AtomicReference]<T>.
 */
@SinceKotlin("2.1")
@Suppress("UNCHECKED_CAST")
@ExperimentalAtomicApi
public fun <T> java.util.concurrent.atomic.AtomicReference<T>.asKotlinAtomic(): AtomicReference<T> = this as AtomicReference<T>