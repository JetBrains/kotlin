/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.contracts

import kotlin.internal.ContractsDsl

/**
 * Abstract effect, the inheritors of which are used in [ContractBuilder] to describe the contract function.
 */
@ContractsDsl
@ExperimentalContracts
@SinceKotlin("1.3")
public interface Effect

/**
 * This interface corresponds to an additional condition applied to the [SimpleEffect].
 * Can only be used in conjunction with [SimpleEffect].
 *
 * @see SimpleEffect.implies
 */
@ContractsDsl
@ExperimentalContracts
@SinceKotlin("1.3")
public interface ConditionalEffect : Effect

/**
 * Describes of function's behavior regardless of any conditions.
 */
@ContractsDsl
@ExperimentalContracts
@SinceKotlin("1.3")
public interface SimpleEffect : Effect {
    /**
     * The function to specify an additional condition for some `SimpleEffect`.
     *
     * @param booleanExpression the additional condition which is a subset of the Kotlin boolean expressions.
     *
     * Note: the subset of the Kotlin boolean expressions is true/false/null checks (using the equality expression) for a function parameter or receiver, also allowed the conjunction and disjunction.
     */
    @ContractsDsl
    @ExperimentalContracts
    public infix fun implies(booleanExpression: Boolean): ConditionalEffect
}

/**
 * Describes the return value of a function.
 * The return value can only be `true`, `false` or `null`.
 *
 * @see ContractBuilder.returns
 */
@ContractsDsl
@ExperimentalContracts
@SinceKotlin("1.3")
public interface Returns : SimpleEffect

/**
 * Describes that the return value of a function is not null.
 *
 * @see ContractBuilder.returnsNotNull
 */
@ContractsDsl
@ExperimentalContracts
@SinceKotlin("1.3")
public interface ReturnsNotNull : SimpleEffect

/**
 * Describes of function's behavior in relation to the invocation of the received lambda.
 *
 * @see ContractBuilder.callsInPlace
 */
@ContractsDsl
@ExperimentalContracts
@SinceKotlin("1.3")
public interface CallsInPlace : Effect