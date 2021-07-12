/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.collections


/** Returns true if the brittle contains optimization is enabled. See KT-45438. */
internal expect fun brittleContainsOptimizationEnabled(): Boolean

/**
 * Returns true if [brittleContainsOptimizationEnabled] is true
 * and it's safe to convert this collection to a set without changing contains method behavior.
 */
private fun <T> Collection<T>.safeToConvertToSet() = brittleContainsOptimizationEnabled() && size > 2 && this is ArrayList

/**
 * When [brittleContainsOptimizationEnabled] is true:
 * - Converts this [Iterable] to a set if it is not a [Collection].
 * - Converts this [Collection] to a set, when it's worth so and it doesn't change contains method behavior.
 * - Otherwise returns this.
 * When [brittleContainsOptimizationEnabled] is false:
 * - Converts this [Iterable] to a list if it is not a [Collection].
 * - Otherwise returns this.
 */
internal fun <T> Iterable<T>.convertToSetForSetOperationWith(source: Iterable<T>): Collection<T> =
    when (this) {
        is Set -> this
        is Collection ->
            when {
                source is Collection && source.size < 2 -> this
                else -> if (this.safeToConvertToSet()) toHashSet() else this
            }
        else -> if (brittleContainsOptimizationEnabled()) toHashSet() else toList()
    }

/**
 * When [brittleContainsOptimizationEnabled] is true:
 * - Converts this [Iterable] to a set if it is not a [Collection].
 * - Converts this [Collection] to a set, when it's worth so and it doesn't change contains method behavior.
 * - Otherwise returns this.
 * When [brittleContainsOptimizationEnabled] is false:
 * - Converts this [Iterable] to a list if it is not a [Collection].
 * - Otherwise returns this.
 */
internal fun <T> Iterable<T>.convertToSetForSetOperation(): Collection<T> =
    when (this) {
        is Set -> this
        is Collection -> if (this.safeToConvertToSet()) toHashSet() else this
        else -> if (brittleContainsOptimizationEnabled()) toHashSet() else toList()
    }

/**
 * Converts this sequence to a set if [brittleContainsOptimizationEnabled] is true,
 * otherwise converts it to a list.
 */
internal fun <T> Sequence<T>.convertToSetForSetOperation(): Collection<T> =
    if (brittleContainsOptimizationEnabled()) toHashSet() else toList()

/**
 * Converts this array to a set if [brittleContainsOptimizationEnabled] is true,
 * otherwise converts it to a list.
 */
internal fun <T> Array<T>.convertToSetForSetOperation(): Collection<T> =
    if (brittleContainsOptimizationEnabled()) toHashSet() else asList()