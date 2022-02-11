/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.util

import java.util.Optional

/**
 * Returns an [empty][Optional.empty] [Optional].
 */
@JvmName("empty")
public fun <T : Any> optionalOf(): Optional<T> = Optional.empty()

/**
 * Returns an [Optional] for the given non-`null` value.
 *
 * Calling this function is equivalent to [Optional.of].
 */
public fun <T : Any> optionalOf(value: T): Optional<T> = Optional.of(value)

/**
 * Returns an [Optional] for the given value if non-`null`, or otherwise [empty][Optional.empty].
 *
 * Calling this function is equivalent to [Optional.ofNullable].
 */
@JvmName("optionalOfNullable")
public fun <T : Any> optionalOf(value: T?): Optional<T> = Optional.ofNullable(value)

/**
 * Returns this [Optional]'s value if [present][Optional.isPresent], or otherwise `null`.
 */
public fun <T : Any> Optional<T>.getOrNull(): T? = orElse(null)

/**
 * Returns this [Optional]'s value if [present][Optional.isPresent], or otherwise [defaultValue].
 */
public fun <T : Any> Optional<T>.getOrDefault(defaultValue: T): T = orElse(defaultValue)

/**
 * Returns this [Optional]'s value if [present][Optional.isPresent], or otherwise the result of the [defaultValue] function.
 */
public inline fun <T : Any> Optional<T>.getOrElse(defaultValue: () -> T): T =
    if (isPresent) get() else defaultValue()

/**
 * Returns a new read-only list of this [Optional]'s value if [present][Optional.isPresent], or otherwise an empty list.
 * The returned list is serializable (JVM).
 */
public fun <T : Any> Optional<T>.toList(): List<T> =
    if (isPresent) listOf(get()) else emptyList()

/**
 * Returns a new read-only set of this [Optional]'s value if [present][Optional.isPresent], or otherwise an empty set.
 * The returned set is serializable (JVM).
 */
public fun <T : Any> Optional<T>.toSet(): Set<T> =
    if (isPresent) setOf(get()) else emptySet()

/**
 * Returns a new sequence for this [Optional]'s value if [present][Optional.isPresent], or otherwise an empty sequence.
 * The returned set is serializable (JVM).
 */
public fun <T : Any> Optional<T>.asSequence(): Sequence<T> =
    if (isPresent) sequenceOf(get()) else emptySequence()
