/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tooling.core

import kotlin.reflect.typeOf

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
    Extras.Key(extrasTypeOf(), name)

fun emptyExtras(): Extras = EmptyExtras

fun extrasOf() = emptyExtras()

fun extrasOf(vararg entries: Extras.Entry<*>): Extras = if (entries.isEmpty()) EmptyExtras else ImmutableExtrasImpl(entries)

fun mutableExtrasOf(): MutableExtras = MutableExtrasImpl()

fun mutableExtrasOf(vararg entries: Extras.Entry<*>): MutableExtras = MutableExtrasImpl(entries.toList())

inline fun <reified T> extrasTypeOf(): Extras.Type<T> {
    @OptIn(UnsafeApi::class, ExperimentalStdlibApi::class)
    return Extras.Type(renderReifiedTypeSignatureString(typeOf<T>()))
}

fun Iterable<Extras.Entry<*>>.toExtras(): Extras = ImmutableExtrasImpl(this)

fun Iterable<Extras.Entry<*>>.toMutableExtras(): MutableExtras = MutableExtrasImpl(this)

infix fun <T : Any> Extras.Key<T>.withValue(value: T): Extras.Entry<T> = Extras.Entry(this, value)

operator fun Extras.plus(entry: Extras.Entry<*>): Extras = ImmutableExtrasImpl(this.entries + entry)

operator fun Extras.plus(entries: Iterable<Extras.Entry<*>>): Extras = ImmutableExtrasImpl(this.entries + entries)

inline fun <T : Any> MutableExtras.getOrPut(key: Extras.Key<T>, defaultValue: () -> T): T {
    return this[key] ?: defaultValue().also { this[key] = it }
}