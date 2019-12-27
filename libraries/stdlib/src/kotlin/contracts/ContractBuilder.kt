/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.contracts

import kotlin.internal.ContractsDsl
import kotlin.internal.InlineOnly

/**
 * This marker distinguishes the experimental contract declaration API and is used to opt-in for that feature
 * when declaring contracts of user functions.
 *
 * Any usage of a declaration annotated with `@ExperimentalContracts` must be accepted either by
 * annotating that usage with the [OptIn] annotation, e.g. `@OptIn(ExperimentalContracts::class)`,
 * or by using the compiler argument `-Xopt-in=kotlin.contracts.ExperimentalContracts`.
 */
@Retention(AnnotationRetention.BINARY)
@SinceKotlin("1.3")
@Experimental
@RequiresOptIn
@MustBeDocumented
public annotation class ExperimentalContracts

/**
 * Provides a scope, where the functions of the contract DSL, such as [returns], [callsInPlace], etc.,
 * can be used to describe the contract of a function.
 *
 * This type is used as a receiver type of the lambda function passed to the [contract] function.
 *
 * @see contract
 */
@ContractsDsl
@ExperimentalContracts
@SinceKotlin("1.3")
public interface ContractBuilder {
    /**
     * Describes a situation when a function returns normally, without any exceptions thrown.
     *
     * Use [SimpleEffect.implies] function to describe a conditional effect that happens in such case.
     *
     */
    // @sample samples.contracts.returnsContract
    @ContractsDsl public fun returns(): Returns

    /**
     * Describes a situation when a function returns normally with the specified return [value].
     *
     * The possible values of [value] are limited to `true`, `false` or `null`.
     *
     * Use [SimpleEffect.implies] function to describe a conditional effect that happens in such case.
     *
     */
    // @sample samples.contracts.returnsTrueContract
    // @sample samples.contracts.returnsFalseContract
    // @sample samples.contracts.returnsNullContract
    @ContractsDsl public fun returns(value: Any?): Returns

    /**
     * Describes a situation when a function returns normally with any value that is not `null`.
     *
     * Use [SimpleEffect.implies] function to describe a conditional effect that happens in such case.
     *
     */
    // @sample samples.contracts.returnsNotNullContract
    @ContractsDsl public fun returnsNotNull(): ReturnsNotNull

    /**
     * Specifies that the function parameter [lambda] is invoked in place.
     *
     * This contract specifies that:
     * 1. the function [lambda] can only be invoked during the call of the owner function,
     *  and it won't be invoked after that owner function call is completed;
     * 2. _(optionally)_ the function [lambda] is invoked the amount of times specified by the [kind] parameter,
     *  see the [InvocationKind] enum for possible values.
     *
     * A function declaring the `callsInPlace` effect must be _inline_.
     *
     */
    /* @sample samples.contracts.callsInPlaceAtMostOnceContract
    * @sample samples.contracts.callsInPlaceAtLeastOnceContract
    * @sample samples.contracts.callsInPlaceExactlyOnceContract
    * @sample samples.contracts.callsInPlaceUnknownContract
    */
    @ContractsDsl public fun <R> callsInPlace(lambda: Function<R>, kind: InvocationKind = InvocationKind.UNKNOWN): CallsInPlace
}

/**
 * Specifies how many times a function invokes its function parameter in place.
 *
 * See [ContractBuilder.callsInPlace] for the details of the call-in-place function contract.
 */
@ContractsDsl
@ExperimentalContracts
@SinceKotlin("1.3")
public enum class InvocationKind {
    /**
     * A function parameter will be invoked one time or not invoked at all.
     */
    // @sample samples.contracts.callsInPlaceAtMostOnceContract
    @ContractsDsl AT_MOST_ONCE,

    /**
     * A function parameter will be invoked one or more times.
     *
     */
    // @sample samples.contracts.callsInPlaceAtLeastOnceContract
    @ContractsDsl AT_LEAST_ONCE,

    /**
     * A function parameter will be invoked exactly one time.
     *
     */
    // @sample samples.contracts.callsInPlaceExactlyOnceContract
    @ContractsDsl EXACTLY_ONCE,

    /**
     * A function parameter is called in place, but it's unknown how many times it can be called.
     *
     */
    // @sample samples.contracts.callsInPlaceUnknownContract
    @ContractsDsl UNKNOWN
}

/**
 * Specifies the contract of a function.
 *
 * The contract description must be at the beginning of a function and have at least one effect.
 *
 * Only the top-level functions can have a contract for now.
 *
 * @param builder the lambda where the contract of a function is described with the help of the [ContractBuilder] members.
 *
 */
/* @sample samples.contracts.returnsContract
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
