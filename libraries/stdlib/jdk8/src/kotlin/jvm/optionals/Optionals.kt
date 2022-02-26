/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.jvm.optionals

import java.util.Optional

/**
 * Returns this [Optional]'s value if [present][Optional.isPresent], or otherwise `null`.
 */
public fun <T : Any> Optional<out T>.getOrNull(): T? = orElse(null)

/**
 * Returns this [Optional]'s value if [present][Optional.isPresent], or otherwise [defaultValue].
 */
public fun <R, T : R & Any> Optional<out T>.getOrDefault(defaultValue: R): R = if (isPresent) get() else defaultValue

/**
 * Returns this [Optional]'s value if [present][Optional.isPresent], or otherwise the result of the [defaultValue] function.
 */
public inline fun <R, T : R & Any> Optional<out T>.getOrElse(defaultValue: () -> R): R =
    if (isPresent) get() else defaultValue()

/**
 * Appends this [Optional]'s value to the given [destination] collection if [present][Optional.isPresent].
 */
public fun <T : Any, C : MutableCollection<in T>> Optional<out T>.toCollection(destination: C): C {
    if (isPresent) {
        destination.add(get())
    }
    return destination
}

/**
 * Returns a new read-only list of this [Optional]'s value if [present][Optional.isPresent], or otherwise an empty list.
 * The returned list is serializable (JVM).
 */
public fun <T : Any> Optional<out T>.toList(): List<T> =
    if (isPresent) listOf(get()) else emptyList()

/**
 * Returns a new read-only set of this [Optional]'s value if [present][Optional.isPresent], or otherwise an empty set.
 * The returned set is serializable (JVM).
 */
public fun <T : Any> Optional<out T>.toSet(): Set<T> =
    if (isPresent) setOf(get()) else emptySet()

/**
 * Returns a new sequence for this [Optional]'s value if [present][Optional.isPresent], or otherwise an empty sequence.
 * The returned set is serializable (JVM).
 */
public fun <T : Any> Optional<out T>.asSequence(): Sequence<T> =
    if (isPresent) sequenceOf(get()) else emptySequence()
