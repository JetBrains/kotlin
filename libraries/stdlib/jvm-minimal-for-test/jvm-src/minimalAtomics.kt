/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.concurrent.atomics

@SinceKotlin("2.2")
@ExperimentalAtomicApi
public actual fun <T> AtomicArray<T>.fetchAndUpdateAt(index: Int, transform: (T) -> T): T = TODO()

@SinceKotlin("2.2")
@ExperimentalAtomicApi
public actual fun <T> AtomicArray<T>.updateAndFetchAt(index: Int, transform: (T) -> T): T = TODO()

@SinceKotlin("2.2")
@ExperimentalAtomicApi
public actual fun <T> AtomicArray<T>.updateAt(index: Int, transform: (T) -> T): Unit = TODO()

@SinceKotlin("2.2")
@ExperimentalAtomicApi
public actual fun AtomicLongArray.fetchAndUpdateAt(index: Int, transform: (Long) -> Long): Long = TODO()

@SinceKotlin("2.2")
@ExperimentalAtomicApi
public actual fun AtomicLongArray.updateAndFetchAt(index: Int, transform: (Long) -> Long): Long = TODO()

@SinceKotlin("2.2")
@ExperimentalAtomicApi
public actual fun AtomicLongArray.updateAt(index: Int, transform: (Long) -> Long): Unit = TODO()

@SinceKotlin("2.2")
@ExperimentalAtomicApi
public actual fun AtomicIntArray.fetchAndUpdateAt(index: Int, transform: (Int) -> Int): Int = TODO()

@SinceKotlin("2.2")
@ExperimentalAtomicApi
public actual fun AtomicIntArray.updateAndFetchAt(index: Int, transform: (Int) -> Int): Int = TODO()

@SinceKotlin("2.2")
@ExperimentalAtomicApi
public actual fun AtomicIntArray.updateAt(index: Int, transform: (Int) -> Int): Unit = TODO()

@SinceKotlin("2.2")
@ExperimentalAtomicApi
public actual fun AtomicInt.update(transform: (Int) -> Int): Unit = TODO()

@SinceKotlin("2.2")
@ExperimentalAtomicApi
public actual fun AtomicInt.fetchAndUpdate(transform: (Int) -> Int): Int = TODO()

@SinceKotlin("2.2")
@ExperimentalAtomicApi
public actual fun AtomicInt.updateAndFetch(transform: (Int) -> Int): Int = TODO()

@SinceKotlin("2.2")
@ExperimentalAtomicApi
public actual fun AtomicLong.update(transform: (Long) -> Long): Unit = TODO()

@SinceKotlin("2.2")
@ExperimentalAtomicApi
public actual fun AtomicLong.fetchAndUpdate(transform: (Long) -> Long): Long = TODO()

@SinceKotlin("2.2")
@ExperimentalAtomicApi
public actual fun AtomicLong.updateAndFetch(transform: (Long) -> Long): Long = TODO()

@SinceKotlin("2.2")
@ExperimentalAtomicApi
public actual fun <T> AtomicReference<T>.update(transform: (T) -> T): Unit = TODO()

@SinceKotlin("2.2")
@ExperimentalAtomicApi
public actual fun <T> AtomicReference<T>.fetchAndUpdate(transform: (T) -> T): T = TODO()

@SinceKotlin("2.2")
@ExperimentalAtomicApi
public actual fun <T> AtomicReference<T>.updateAndFetch(transform: (T) -> T): T = TODO()
