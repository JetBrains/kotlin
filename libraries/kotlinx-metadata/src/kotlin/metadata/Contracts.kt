/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.metadata

import kotlin.contracts.ExperimentalContracts

/**
 * Represents a contract of a Kotlin function.
 *
 * Contracts are an internal feature of the standard Kotlin library, and their behavior and/or binary format
 * may change in a subsequent release.
 */
@ExperimentalContracts
public class KmContract {
    /**
     * Effects of this contract.
     */
    public val effects: MutableList<KmEffect> = ArrayList(1)
}

/**
 * Represents an effect (a part of the contract of a Kotlin function).
 *
 * Contracts are an internal feature of the standard Kotlin library, and their behavior and/or binary format
 * may change in a subsequent release.
 *
 * @property type type of the effect
 * @property invocationKind optional number of invocations of the lambda parameter of this function,
 *   specified further in the effect expression
 */
@ExperimentalContracts
public class KmEffect(
    public var type: KmEffectType,
    public var invocationKind: KmEffectInvocationKind?,
) {
    /**
     * Arguments of the effect constructor, i.e., the constant value for the [KmEffectType.RETURNS_CONSTANT] effect,
     * or the parameter reference for the [KmEffectType.CALLS] effect.
     */
    public val constructorArguments: MutableList<KmEffectExpression> = ArrayList(1)

    /**
     * Conclusion of the effect. If this value is set, the effect represents an implication with this value as the right-hand side.
     */
    public var conclusion: KmEffectExpression? = null
}

/**
 * Represents an effect expression, the contents of an effect (a part of the contract of a Kotlin function).
 *
 * Contracts are an internal feature of the standard Kotlin library, and their behavior and/or binary format
 * may change in a subsequent release.
 *
 * Various effect expression attributes can be read and manipulated via extension properties,
 * such as [KmEffectExpression.isNegated].
 */
@ExperimentalContracts
public class KmEffectExpression {
    internal var flags: Int = 0

    /**
     * Optional 1-based index of the value parameter of the function, for effects which assert something about
     * the function parameters. Index 0 means the extension receiver parameter.
     */
    public var parameterIndex: Int? = null

    /**
     * Constant value used in the effect expression.
     */
    public var constantValue: KmConstantValue? = null

    /**
     * Type used as the target of an `is`-expression in the effect expression.
     */
    public var isInstanceType: KmType? = null

    /**
     * Arguments of an `&&`-expression. If this list is non-empty, the resulting effect expression is a conjunction of this expression
     * and elements of the list.
     */
    public val andArguments: MutableList<KmEffectExpression> = ArrayList(0)

    /**
     * Arguments of an `||`-expression. If this list is non-empty, the resulting effect expression is a disjunction of this expression
     * and elements of the list.
     */
    public val orArguments: MutableList<KmEffectExpression> = ArrayList(0)
}

/**
 * Represents a constant value used in an effect expression.
 *
 * Contracts are an internal feature of the standard Kotlin library, and their behavior and/or binary format
 * may change in a subsequent release.
 *
 * @property value the constant value. May be `true`, `false` or `null`
 */
@ExperimentalContracts
public data class KmConstantValue(val value: Any?)


/**
 * Type of an effect (a part of the contract of a Kotlin function).
 *
 * Contracts are an internal feature of the standard Kotlin library, and their behavior and/or binary format
 * may change in a subsequent release.
 */
@ExperimentalContracts
public enum class KmEffectType {
    /**
     * Represents `returns(value)` contract effect:
     * a situation when a function returns normally with the specified return value.
     * Return value is stored in the [KmEffect.constructorArguments].
     */
    RETURNS_CONSTANT,

    /**
     * Represents `callsInPlace` contract effect:
     * A situation when the referenced lambda is invoked in place (optionally) specified number of times.
     *
     * Referenced lambda is stored in the [KmEffect.constructorArguments].
     * Number of invocations, if specified, is stored in [KmEffect.invocationKind].
     */
    CALLS,

    /**
     * Represents `returnsNotNull` contract effect:
     * a situation when a function returns normally with any value that is not null.
     */
    RETURNS_NOT_NULL,
}

/**
 * Number of invocations of a lambda parameter specified by an effect (a part of the contract of a Kotlin function).
 *
 * Contracts are an internal feature of the standard Kotlin library, and their behavior and/or binary format
 * may change in a subsequent release.
 */
@ExperimentalContracts
public enum class KmEffectInvocationKind {
    /**
     * A function parameter will be invoked one time or not invoked at all.
     */
    AT_MOST_ONCE,

    /**
     * A function parameter will be invoked exactly one time.
     */
    EXACTLY_ONCE,

    /**
     * A function parameter will be invoked one or more times.
     */
    AT_LEAST_ONCE,
}
