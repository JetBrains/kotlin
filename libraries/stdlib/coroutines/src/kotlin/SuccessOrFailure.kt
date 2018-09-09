/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

// todo: Figure out how to avoid suppressing errors, move suppressions where they are needed.
@file:Suppress(
    "UNCHECKED_CAST",
    "RedundantVisibilityModifier",
    "NON_PUBLIC_PRIMARY_CONSTRUCTOR_OF_INLINE_CLASS",
    "UNSUPPORTED_FEATURE"
)

package kotlin

import kotlin.contracts.*
import kotlin.internal.InlineOnly
import kotlin.jvm.JvmField

// TODO: REMOVE AFTER BOOTSTRAP

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
    @Suppress("RESERVED_MEMBER_INSIDE_INLINE_CLASS")
    public override fun equals(other: Any?): Boolean = other is SuccessOrFailure<*> && value == other.value

    /**
     * Returns hashcode of either the encapsulated value or of the exception.
     */
    @Suppress("RESERVED_MEMBER_INSIDE_INLINE_CLASS")
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
