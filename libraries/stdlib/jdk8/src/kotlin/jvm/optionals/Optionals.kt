/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
package kotlin.jvm.optionals

import java.util.Optional

/**
 * Returns this [Optional]'s value if [present][Optional.isPresent], or otherwise `null`.
 */
@SinceKotlin("1.8")
@WasExperimental(ExperimentalStdlibApi::class)
public fun <T : Any> Optional<T>.getOrNull(): T? = orElse(null)

/**
 * Returns this [Optional]'s value if [present][Optional.isPresent], or otherwise [defaultValue].
 */
@SinceKotlin("1.8")
@WasExperimental(ExperimentalStdlibApi::class)
public fun <T> Optional<out T & Any>.getOrDefault(defaultValue: T): T = if (isPresent) get() else defaultValue

/**
 * Returns this [Optional]'s value if [present][Optional.isPresent], or otherwise the result of the [defaultValue] function.
 */
@SinceKotlin("1.8")
@WasExperimental(ExperimentalStdlibApi::class)
public inline fun <T> Optional<out T & Any>.getOrElse(defaultValue: () -> T): T =
    if (isPresent) get() else defaultValue()

/**
 * Appends this [Optional]'s value to the given [destination] collection if [present][Optional.isPresent].
 */
@SinceKotlin("1.8")
@WasExperimental(ExperimentalStdlibApi::class)
public fun <T : Any, C : MutableCollection<in T>> Optional<T>.toCollection(destination: C): C {
    if (isPresent) {
        destination.add(get())
    }
    return destination
}

/**
 * Returns a new read-only list of this [Optional]'s value if [present][Optional.isPresent], or otherwise an empty list.
 * The returned list is serializable (JVM).
 */
@SinceKotlin("1.8")
@WasExperimental(ExperimentalStdlibApi::class)
public fun <T : Any> Optional<out T>.toList(): List<T> =
    if (isPresent) listOf(get()) else emptyList()

/**
 * Returns a new read-only set of this [Optional]'s value if [present][Optional.isPresent], or otherwise an empty set.
 * The returned set is serializable (JVM).
 */
@SinceKotlin("1.8")
@WasExperimental(ExperimentalStdlibApi::class)
public fun <T : Any> Optional<out T>.toSet(): Set<T> =
    if (isPresent) setOf(get()) else emptySet()

/**
 * Returns a new sequence for this [Optional]'s value if [present][Optional.isPresent], or otherwise an empty sequence.
 * The returned set is serializable (JVM).
 */
@SinceKotlin("1.8")
@WasExperimental(ExperimentalStdlibApi::class)
public fun <T : Any> Optional<out T>.asSequence(): Sequence<T> =
    if (isPresent) sequenceOf(get()) else emptySequence()
