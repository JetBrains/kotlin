/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tooling.core

inline fun <reified T : Any> extraKey(name: String? = null): Extras.Key<T> {
    return Extras.Key(Extras.Id(reifiedTypeSignatureOf(), name))
}

fun emptyExtras(): IterableExtras = EmptyExtras

fun extrasOf() = emptyExtras()

fun extrasOf(vararg entries: Extras.Entry<*>): IterableExtras = ImmutableExtrasImpl(entries.toList())

fun mutableExtrasOf(): MutableExtras = MutableExtrasImpl()

fun mutableExtrasOf(vararg entries: Extras.Entry<*>): MutableExtras = MutableExtrasImpl(entries.toList())

fun Iterable<Extras.Entry<*>>.toExtras(): IterableExtras = ImmutableExtrasImpl(this)

fun Iterable<Extras.Entry<*>>.toMutableExtras(): MutableExtras = MutableExtrasImpl(this)

infix fun <T : Any> Extras.Key<T>.withValue(value: T): Extras.Entry<T> = Extras.Entry(this, value)

operator fun IterableExtras.plus(entry: Extras.Entry<*>): IterableExtras = ImmutableExtrasImpl(this.entries + entry)

operator fun IterableExtras.plus(entries: Iterable<Extras.Entry<*>>): IterableExtras = ImmutableExtrasImpl(this.entries + entries)
