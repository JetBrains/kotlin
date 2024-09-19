/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package kotlin.concurrent

import java.util.concurrent.atomic.*

/**
 * Casts the given [AtomicInt] instance to [java.util.concurrent.atomic.AtomicInteger].
 */
@Suppress("UNCHECKED_CAST")
public fun AtomicInt.asJavaAtomic(): AtomicInteger = this as AtomicInteger

/**
 * Casts the given [java.util.concurrent.atomic.AtomicInteger] instance to [AtomicInt].
 */
@Suppress("UNCHECKED_CAST")
public fun AtomicInteger.asKotlinAtomic(): AtomicInt = this as AtomicInt

/**
 * Casts the given [AtomicLong] instance to [java.util.concurrent.atomic.AtomicLong].
 */
@Suppress("UNCHECKED_CAST")
public fun AtomicLong.asJavaAtomic(): java.util.concurrent.atomic.AtomicLong = this as java.util.concurrent.atomic.AtomicLong

/**
 * Casts the given [java.util.concurrent.atomic.AtomicLong] instance to [AtomicLong].
 */
@Suppress("UNCHECKED_CAST")
public fun java.util.concurrent.atomic.AtomicLong.asKotlinAtomic(): AtomicLong = this as AtomicLong

/**
 * Casts the given [AtomicBoolean] instance to [java.util.concurrent.atomic.AtomicBoolean].
 */
@Suppress("UNCHECKED_CAST")
public fun <T> AtomicBoolean.asJavaAtomic(): java.util.concurrent.atomic.AtomicBoolean = this as java.util.concurrent.atomic.AtomicBoolean

/**
 * Casts the given [java.util.concurrent.atomic.AtomicBoolean] instance to [AtomicBoolean].
 */
@Suppress("UNCHECKED_CAST")
public fun <T> java.util.concurrent.atomic.AtomicBoolean.asKotlinAtomic(): AtomicBoolean = this as AtomicBoolean

/**
 * Casts the given [AtomicReference]<T> instance to [java.util.concurrent.atomic.AtomicReference]<T>.
 */
@Suppress("UNCHECKED_CAST")
public fun <T> AtomicReference<T>.asJavaAtomic(): java.util.concurrent.atomic.AtomicReference<T> = this as java.util.concurrent.atomic.AtomicReference<T>

/**
 * Casts the given [java.util.concurrent.atomic.AtomicReference]<T> instance to [AtomicReference]<T>.
 */
@Suppress("UNCHECKED_CAST")
public fun <T> java.util.concurrent.atomic.AtomicReference<T>.asKotlinAtomic(): AtomicReference<T> = this as AtomicReference<T>