/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.contracts

import kotlin.internal.ContractsDsl
import kotlin.internal.InlineOnly

/**
 * This marker distinguishes the experimental contract declaration API and is used to opt-in for that feature
 * when declaring contracts of user functions.
 *
 * Any usage of a declaration annotated with `@ExperimentalContracts` must be accepted either by
 * annotating that usage with the [UseExperimental] annotation, e.g. `@UseExperimental(ExperimentalContracts::class)`,
 * or by using the compiler argument `-Xuse-experimental=kotlin.contracts.ExperimentalContracts`.
 */
@Retention(AnnotationRetention.BINARY)
@SinceKotlin("1.3")
@Experimental
public annotation class ExperimentalContracts

/**
 * The builder is used to specify contract effects for some function.
 *
 * @see contract
 */
@ContractsDsl
@ExperimentalContracts
@SinceKotlin("1.3")
public interface ContractBuilder {
    /**
     * Expresses that a function returned successfully.
     *
     * @sample samples.contracts.returnsContract
     */
    @ContractsDsl public fun returns(): Returns

    /**
     * Expresses that a function returned with some value.
     * Value can only be `true`, `false` or `null`.
     *
     * @sample samples.contracts.returnsTrueContract
     * @sample samples.contracts.returnsFalseContract
     * @sample samples.contracts.returnsNullContract
     */
    @ContractsDsl public fun returns(value: Any?): Returns

    /**
     * Expresses that a function returned with any not null value.
     *
     * @sample samples.contracts.returnsNotNullContract
     */
    @ContractsDsl public fun returnsNotNull(): ReturnsNotNull

    /**
     * Expresses that:
     *  1) [lambda] will not be called after the call to owner-function is finished;
     *  2) [lambda] will not be passed to another function without the similar contract.
     *
     * @param kind amount of times, that a [lambda] guaranteed will be invoked.
     *
     * Note that a function with the `callsInPlace` effect must be inline.
     *
     * @sample samples.contracts.callsInPlaceAtMostOnceContract
     * @sample samples.contracts.callsInPlaceAtLeastOnceContract
     * @sample samples.contracts.callsInPlaceExactlyOnceContract
     * @sample samples.contracts.callsInPlaceUnknownContract
     */
    @ContractsDsl public fun <R> callsInPlace(lambda: Function<R>, kind: InvocationKind = InvocationKind.UNKNOWN): CallsInPlace
}

/**
 * This enum class is used to specify the amount of times, that `callable` which is used in the [ContractBuilder.callsInPlace] effect guaranteed will be invoked.
 */
@ContractsDsl
@ExperimentalContracts
@SinceKotlin("1.3")
public enum class InvocationKind {
    /**
     * Expresses that the `callable` will be invoked zero or one time.
     *
     * @sample samples.contracts.callsInPlaceAtMostOnceContract
     */
    @ContractsDsl AT_MOST_ONCE,

    /**
     * Expresses that the `callable` will be invoked one or more times.
     *
     * @sample samples.contracts.callsInPlaceAtLeastOnceContract
     */
    @ContractsDsl AT_LEAST_ONCE,

    /**
     * Expresses that the `callable` will be invoked exactly one time.
     *
     * @sample samples.contracts.callsInPlaceExactlyOnceContract
     */
    @ContractsDsl EXACTLY_ONCE,

    /**
     * Expresses that the `callable` will be invoked unknown amount of times.
     *
     * @sample samples.contracts.callsInPlaceUnknownContract
     */
    @ContractsDsl UNKNOWN
}

/**
 * The function to describe a contract.
 * The contract description must be at the beginning of a function and has at least one effect.
 * Also the contract description can be used only in the top-level functions.
 *
 * @param builder the lambda in the body of which the effects from the [ContractBuilder] are specified.
 *
 * @sample samples.contracts.returnsContract
 * @sample samples.contracts.returnsTrueContract
 * @sample samples.contracts.returnsFalseContract
 * @sample samples.contracts.returnsNullContract
 * @sample samples.contracts.returnsNotNullContract
 * @sample samples.contracts.callsInPlaceAtMostOnceContract
 * @sample samples.contracts.callsInPlaceAtLeastOnceContract
 * @sample samples.contracts.callsInPlaceExactlyOnceContract
 * @sample samples.contracts.callsInPlaceUnknownContract
 */
@ContractsDsl
@ExperimentalContracts
@InlineOnly
@SinceKotlin("1.3")
@Suppress("UNUSED_PARAMETER")
public inline fun contract(builder: ContractBuilder.() -> Unit) { }