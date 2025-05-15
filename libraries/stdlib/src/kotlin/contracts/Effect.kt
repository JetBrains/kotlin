/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.contracts

import kotlin.internal.ContractsDsl

/**
 * Represents an effect of a function invocation,
 * either directly observable, such as the function returning normally,
 * or a side-effect, such as the function's lambda parameter being called in place.
 *
 * The inheritors are used in [ContractBuilder] to describe the contract of a function.
 *
 * @see ConditionalEffect
 * @see SimpleEffect
 * @see CallsInPlace
 */
@ContractsDsl
@ExperimentalContracts
@SinceKotlin("1.3")
public interface Effect

/**
 * An effect of some condition being true after observing another effect of a function.
 *
 * This effect is specified in the `contract { }` block by attaching a boolean expression
 * to another [SimpleEffect] effect with the function [SimpleEffect.implies].
 */
@ContractsDsl
@ExperimentalContracts
@SinceKotlin("1.3")
public interface ConditionalEffect : Effect

/**
 * An effect that can be observed after a function invocation.
 *
 * @see ContractBuilder.returns
 * @see ContractBuilder.returnsNotNull
 */
@ContractsDsl
@ExperimentalContracts
@SinceKotlin("1.3")
public interface SimpleEffect : Effect {
    /**
     * Specifies that this effect, when observed, guarantees [booleanExpression] to be true.
     *
     * Note: [booleanExpression] can accept only a subset of boolean expressions,
     * where a function parameter or receiver (`this`) undergoes
     * - true of false checks, in case if the parameter or receiver is `Boolean`;
     * - null-checks (`== null`, `!= null`);
     * - instance-checks (`is`, `!is`);
     * - a combination of the above with the help of logic operators (`&&`, `||`, `!`).
     */
    @ContractsDsl
    @ExperimentalContracts
    @IgnorableReturnValue
    public infix fun implies(booleanExpression: Boolean): ConditionalEffect
}

/**
 * Describes a situation when a function returns normally with a given return value.
 *
 * @see ContractBuilder.returns
 */
@ContractsDsl
@ExperimentalContracts
@SinceKotlin("1.3")
public interface Returns : SimpleEffect

/**
 * Describes a situation when a function returns normally with any non-null return value.
 *
 * @see ContractBuilder.returnsNotNull
 */
@ContractsDsl
@ExperimentalContracts
@SinceKotlin("1.3")
public interface ReturnsNotNull : SimpleEffect

/**
 * An effect of calling a functional parameter in place.
 *
 * A function is said to call its functional parameter in place, if the functional parameter is only invoked
 * while the execution has not been returned from the function, and the functional parameter cannot be
 * invoked after the function is completed.
 *
 * @see ContractBuilder.callsInPlace
 */
@ContractsDsl
@ExperimentalContracts
@SinceKotlin("1.3")
public interface CallsInPlace : Effect
