/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tooling.core

/**
 *  Creates a value based key for accessing any [Extras] container
 *
 * @param T The type of data that is stored in the extras container
 * ```kotlin
 * extrasKeyOf<Int>() == extrasKeyOf<Int>()
 * extrasKeyOf<Int>() != extrasKeyOf<String>()
 * extrasKeyOf<List<Int>>() == extrasKeyOf<List<Int>>()
 * extrasKeyOf<List<*>>() != extrasKeyOf<List<Int>>()
 * ```
 *
 * @param name This typed keys can also be distinguished with an additional name. In this case
 * ```kotlin
 * extrasKeyOf<Int>() != extrasKeyOf<Int>("a")
 * extrasKeyOf<Int>("a") == extrasKeyOf<Int>("a")
 * extrasKeyOf<Int>("b") != extrasKeyOf<Int>("a")
 * extrasKeyOf<String>("a") != extrasKeyOf<Int>("a")
 * ```
 */
inline fun <reified T : Any> extrasKeyOf(name: String? = null): Extras.Key<T> =
    Extras.Key(extrasIdOf(name))

inline fun <reified T : Any> extrasIdOf(name: String? = null): Extras.Id<T> =
    Extras.Id(reifiedTypeSignatureOf(), name)

fun emptyExtras(): IterableExtras = EmptyExtras

fun extrasOf() = emptyExtras()

fun extrasOf(vararg entries: Extras.Entry<*>): IterableExtras = ImmutableExtrasImpl(entries)

fun mutableExtrasOf(): MutableExtras = MutableExtrasImpl()

fun mutableExtrasOf(vararg entries: Extras.Entry<*>): MutableExtras = MutableExtrasImpl(entries.toList())

fun Iterable<Extras.Entry<*>>.toExtras(): IterableExtras = ImmutableExtrasImpl(this)

fun Iterable<Extras.Entry<*>>.toMutableExtras(): MutableExtras = MutableExtrasImpl(this)

infix fun <T : Any> Extras.Key<T>.withValue(value: T): Extras.Entry<T> = Extras.Entry(this, value)

operator fun IterableExtras.plus(entry: Extras.Entry<*>): IterableExtras = ImmutableExtrasImpl(this.entries + entry)

operator fun IterableExtras.plus(entries: Iterable<Extras.Entry<*>>): IterableExtras = ImmutableExtrasImpl(this.entries + entries)

/**
 * Filters the given [Extras] by exact type matches.
 * Contrary to operations like [filterIsInstance], this operation is invariant under [T] and will
 * only filter for entries stored exactly as [T]
 */
inline fun <reified T : Any> IterableExtras.filterType(): Iterable<Extras.Entry<T>> {
    return filterType(reifiedTypeSignatureOf())
}

@PublishedApi
@Suppress("unchecked_cast")
internal fun <T : Any> IterableExtras.filterType(type: ReifiedTypeSignature<T>): Iterable<Extras.Entry<T>> =
    filter { entry -> entry.key.id.type == type } as Iterable<Extras.Entry<T>>

