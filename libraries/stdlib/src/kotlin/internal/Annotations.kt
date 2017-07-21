/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kotlin.internal

import kotlin.reflect.KClass

/**
 * Specifies that the corresponding type should be ignored during type inference.
 */
@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.BINARY)
internal annotation class NoInfer

/**
 * Specifies that the constraint built for the type during type inference should be an equality one.
 */
@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.BINARY)
internal annotation class Exact

/**
 * Specifies that a corresponding member has the lowest priority in overload resolution.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.BINARY)
internal annotation class LowPriorityInOverloadResolution

/**
 * Specifies that the corresponding member has the highest priority in overload resolution. Effectively this means that
 * an extension annotated with this annotation will win in overload resolution over a member with the same signature.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.BINARY)
internal annotation class HidesMembers

/**
 * The value of this type parameter should be mentioned in input types (argument types, receiver type or expected type).
 */
@Target(AnnotationTarget.TYPE_PARAMETER)
@Retention(AnnotationRetention.BINARY)
internal annotation class OnlyInputTypes

/**
 * Specifies that this function should not be called directly without inlining
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.BINARY)
internal annotation class InlineOnly

/**
 * Specifies that this declaration can have dynamic receiver type.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.BINARY)
internal annotation class DynamicExtension

/**
 * Specifies effect: annotated function returns specific value when condition provided
 * by other annotations is true.*
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
internal annotation class Returns(val value: ConstantValue = ConstantValue.UNKNOWN)

/**
 * Specifies effect: annotated function throws 'exception' when condition is true
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
internal annotation class Throws

/**
 * Specifies effect: annotated callable value parameter will be called 'count' amount of times in-place.
 * "In-place" means that enclosing function guarantees that this callable parameter won't be called after
 * function finished.
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.BINARY)
internal annotation class CalledInPlace(val count: InvocationCount = InvocationCount.UNKNOWN)


/**
 * Specifies how atomic conditions for annotated function should be merged into single condition
 * that will be used to determine if the effect was fired.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
internal annotation class JoinConditions(val strategy: JoiningStrategy = JoiningStrategy.ALL)
internal enum class JoiningStrategy {
    /** At least one of the atomic conditions should be true, i.e. atomic conditions are joined with logic OR */
    ANY,

    /** None of the atomic conditions should be true, i.e. atomic conditions
     * are joined with OR and then whole logic expression is negated
     */
    NONE,

    /** All of the atomic conditions should be true, i.e. atomic conditions are joined with AND */
    ALL
}

/**
 * Adds atomic condition of form 'target == value'
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.BINARY)
internal annotation class Equals(val value: ConstantValue)

/**
 * Adds atomic condition of form 'target is klass'
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.BINARY)
internal annotation class IsInstance(val klass: KClass<*>)

/**
 * Inverts ALL atomic conditions on target
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.BINARY)
internal annotation class Not

enum class ConstantValue {
    TRUE,
    FALSE,
    NULL,
    NOT_NULL,
    UNKNOWN
}

enum class InvocationCount {
    /** Guaranteed to be invoked either 0 or 1 time */
    AT_MOST_ONCE,

    /** Guaranteed to be invoked exactly 1 time */
    EXACTLY_ONCE,

    /** Guaranteed to be invoked once, but may be invoked more */
    AT_LEAST_ONCE,

    /** Exact number of invocations is unknown */
    UNKNOWN
}
