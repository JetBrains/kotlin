/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

// todo: Figure out how to avoid suppressing errors, move suppressions where they are needed.
@file:Suppress(
    "UNCHECKED_CAST",
    "RedundantVisibilityModifier",
    "NON_PUBLIC_PRIMARY_CONSTRUCTOR_OF_INLINE_CLASS",
    "UNSUPPORTED_FEATURE",
    "INVISIBLE_REFERENCE",
    "INVISIBLE_MEMBER",
    "CANNOT_OVERRIDE_INVISIBLE_MEMBER"
)

package kotlin

import kotlin.contracts.*
import kotlin.internal.InlineOnly
import kotlin.jvm.JvmField

/**
 * A discriminated union that encapsulates successful outcome with a value of type [T]
 * or a failure with an arbitrary [Throwable] exception.
 */
public inline class SuccessOrFailure<out T> @PublishedApi internal constructor(
    @PublishedApi internal val value: Any?
) : Serializable {
    // discovery

    /**
     * Returns `true` if this instance represents successful outcome.
     * In this case [isFailure] returns `false`.
     */
    public val isSuccess: Boolean get() = value !is Failure

    /**
     * Returns `true` if this instance represents failed outcome.
     * In this case [isSuccess] returns `false`.
     */
    public val isFailure: Boolean get() = value is Failure

    // value retrieval

    /**
     * Returns the encapsulated value if this instance represents [success][isSuccess] or throws the encapsulated exception
     * if it is [failure][isFailure].
     *
     * This function is shorthand for `getOrElse { throw it }` (see [getOrElse]).
     */
    public fun getOrThrow(): T =
        when (value) {
            is Failure -> throw value.exception
            else -> value as T
        }

    /**
     * Returns the encapsulated value if this instance represents [success][isSuccess] or `null`
     * if it is [failure][isFailure].
     *
     * This function is shorthand for `getOrElse { null }` (see [getOrElse]).
     */
    public fun getOrNull(): T? =
        when (value) {
            is Failure -> null
            else -> value as T
        }

    // exception retrieval

    /**
     * Returns the encapsulated exception if this instance represents [failure][isFailure] or `null`
     * if it is [success][isSuccess].
     *
     * This function is shorthand for `fold(onSuccess = { null }, onFailure = { it })` (see [fold]).
     */
    public fun exceptionOrNull(): Throwable? =
        when (value) {
            is Failure -> value.exception
            else -> null
        }

    // identity

    /**
     * Returns `true` if the [other] object is `SuccessOrFailure` that encapsulates an equal value or exception.
     */
    public override fun equals(other: Any?): Boolean = other is SuccessOrFailure<*> && value == other.value

    /**
     * Returns hashcode of either the encapsulated value or of the exception.
     */
    public override fun hashCode(): Int = value?.hashCode() ?: 0

    /**
     * Returns a string representation of the encapsulated value or `Failure(xxx)` string where
     * `xxx` is a string representation of the exception.
     */
    public override fun toString(): String = value.toString()

    // companion with constructors

    /**
     * Companion object for [SuccessOrFailure] class that contains its constructor functions
     * [success] and [failure].
     */
    public companion object {
        /**
         * Returns an instance that encapsulates the given [value] as successful value.
         */
        @InlineOnly public inline fun <T> success(value: T): SuccessOrFailure<T> =
            SuccessOrFailure(value)

        /**
         * Returns an instance that encapsulates the given [exception] as failure.
         */
        @InlineOnly public inline fun <T> failure(exception: Throwable): SuccessOrFailure<T> =
            SuccessOrFailure(Failure(exception))
    }

    @PublishedApi
    internal class Failure @PublishedApi internal constructor(
        @JvmField
        val exception: Throwable
    ) : Serializable {
        override fun equals(other: Any?): Boolean = other is Failure && exception == other.exception
        override fun hashCode(): Int = exception.hashCode()
        override fun toString(): String = "Failure($exception)"
    }
}

/**
 * Calls the specified function [block] and returns its encapsulated result if invocation was successful,
 * catching and encapsulating any thrown exception as a failure.
 */
@InlineOnly public inline fun <R> runCatching(block: () -> R): SuccessOrFailure<R> {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return try {
        SuccessOrFailure.success(block())
    } catch (e: Throwable) {
        SuccessOrFailure.failure(e)
    }
}

/**
 * Calls the specified function [block] with `this` value as its receiver and returns its encapsulated result
 * if invocation was successful, catching and encapsulating any thrown exception as a failure.
 */
@InlineOnly public inline fun <T, R> T.runCatching(block: T.() -> R): SuccessOrFailure<R> {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return try {
        SuccessOrFailure.success(block())
    } catch (e: Throwable) {
        SuccessOrFailure.failure(e)
    }
}

// -- extensions ---

/**
 * Returns the encapsulated value if this instance represents [success][SuccessOrFailure.isSuccess] or the
 * result of [onFailure] function for encapsulated exception if it is [failure][SuccessOrFailure.isFailure].
 *
 * Note, that an exception thrown by [onFailure] function is rethrown by this function.
 *
 * This function is shorthand for `fold(onSuccess = { it }, onFailure = onFailure)` (see [fold]).
 */
@InlineOnly public inline fun <R, T : R> SuccessOrFailure<T>.getOrElse(onFailure: (exception: Throwable) -> R): R {
    contract {
        callsInPlace(onFailure, InvocationKind.AT_MOST_ONCE)
    }
    return when(value) {
        is SuccessOrFailure.Failure -> onFailure(value.exception)
        else -> value as T
    }
}

/**
 * Returns the encapsulated value if this instance represents [success][SuccessOrFailure.isSuccess] or the
 * [defaultValue] if it is [failure][SuccessOrFailure.isFailure].
 *
 * This function is shorthand for `getOrElse { defaultValue }` (see [getOrElse]).
 */
public fun <R, T : R> SuccessOrFailure<T>.getOrDefault(defaultValue: R): R =
    getOrElse { defaultValue }

/**
 * Returns the the result of [onSuccess] for encapsulated value if this instance represents [success][SuccessOrFailure.isSuccess]
 * or the result of [onFailure] function for encapsulated exception if it is [failure][SuccessOrFailure.isFailure].
 *
 * Note, that an exception thrown by [onSuccess] or by [onFailure] function is rethrown by this function.
 */
@InlineOnly public inline fun <R, T> SuccessOrFailure<T>.fold(
    onSuccess: (value: T) -> R,
    onFailure: (exception: Throwable) -> R
): R {
    contract {
        callsInPlace(onSuccess, InvocationKind.AT_MOST_ONCE)
        callsInPlace(onFailure, InvocationKind.AT_MOST_ONCE)
    }
    return when(value) {
        is SuccessOrFailure.Failure -> onFailure(value.exception)
        else -> onSuccess(value as T)
    }
}

// transformation

/**
 * Returns the encapsulated result of the given [transform] function applied to encapsulated value
 * if this instance represents [success][SuccessOrFailure.isSuccess] or the
 * original encapsulated exception if it is [failure][SuccessOrFailure.isFailure].
 *
 * Note, that an exception thrown by [transform] function is rethrown by this function.
 * See [mapCatching] for an alternative that encapsulates exceptions.
 */
@InlineOnly public inline fun <R, T> SuccessOrFailure<T>.map(transform: (value: T) -> R): SuccessOrFailure<R> {
    contract {
        callsInPlace(transform, InvocationKind.AT_MOST_ONCE)
    }
    return when(value) {
        is SuccessOrFailure.Failure -> SuccessOrFailure(value)
        else -> SuccessOrFailure.success(transform(value as T))
    }
}

/**
 * Returns the encapsulated result of the given [transform] function applied to encapsulated value
 * if this instance represents [success][SuccessOrFailure.isSuccess] or the
 * original encapsulated exception if it is [failure][SuccessOrFailure.isFailure].
 *
 * Any exception thrown by [transform] function is caught, encapsulated as a failure and returned by this function.
 * See [map] for an alternative that rethrows exceptions.
 */
@InlineOnly public inline fun <R, T> SuccessOrFailure<T>.mapCatching(transform: (value: T) -> R): SuccessOrFailure<R> {
    contract {
        callsInPlace(transform, InvocationKind.AT_MOST_ONCE)
    }
    return when(value) {
        is SuccessOrFailure.Failure -> SuccessOrFailure(value)
        else -> runCatching { transform(value as T) }
    }
}

/**
 * Returns the encapsulated result of the given [transform] function applied to encapsulated exception
 * if this instance represents [failure][SuccessOrFailure.isFailure] or the
 * original encapsulated value if it is [success][SuccessOrFailure.isSuccess].
 *
 * Note, that an exception thrown by [transform] function is rethrown by this function.
 * See [recoverCatching] for an alternative that encapsulates exceptions.
 */
@InlineOnly public inline fun <R, T: R> SuccessOrFailure<T>.recover(transform: (exception: Throwable) -> R): SuccessOrFailure<R> {
    contract {
        callsInPlace(transform, InvocationKind.AT_MOST_ONCE)
    }
    return when(value) {
        is SuccessOrFailure.Failure -> SuccessOrFailure.success(transform(value.exception))
        else -> this
    }
}

/**
 * Returns the encapsulated result of the given [transform] function applied to encapsulated exception
 * if this instance represents [failure][SuccessOrFailure.isFailure] or the
 * original encapsulated value if it is [success][SuccessOrFailure.isSuccess].
 *
 * Any exception thrown by [transform] function is caught, encapsulated as a failure and returned by this function.
 * See [recover] for an alternative that rethrows exceptions.
 */
@InlineOnly public inline fun <R, T: R> SuccessOrFailure<T>.recoverCatching(transform: (exception: Throwable) -> R): SuccessOrFailure<R> {
    contract {
        callsInPlace(transform, InvocationKind.AT_MOST_ONCE)
    }
    val value = value // workaround for inline classes BE bug
    return when(value) {
        is SuccessOrFailure.Failure -> runCatching { transform(value.exception) }
        else -> this
    }
}

// "peek" onto value/exception and pipe

/**
 * Performs the given [action] on encapsulated value if this instance represents [success][SuccessOrFailure.isSuccess].
 * Returns the original `SuccessOrFailure` unchanged.
 */
@InlineOnly public inline fun <T> SuccessOrFailure<T>.onFailure(action: (exception: Throwable) -> Unit): SuccessOrFailure<T> {
    contract {
        callsInPlace(action, InvocationKind.AT_MOST_ONCE)
    }
    if (value is SuccessOrFailure.Failure) action(value.exception)
    return this
}

/**
 * Performs the given [action] on encapsulated exception if this instance represents [failure][SuccessOrFailure.isFailure].
 * Returns the original `SuccessOrFailure` unchanged.
 */
@InlineOnly public inline fun <T> SuccessOrFailure<T>.onSuccess(action: (value: T) -> Unit): SuccessOrFailure<T> {
    contract {
        callsInPlace(action, InvocationKind.AT_MOST_ONCE)
    }
    if (value !is SuccessOrFailure.Failure) action(value as T)
    return this
}

// -------------------