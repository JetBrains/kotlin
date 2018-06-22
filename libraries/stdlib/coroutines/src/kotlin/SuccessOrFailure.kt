/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

// todo: Figure out how to avoid supressing error, move suppressions where they are needed.
@file:Suppress(
    "UNCHECKED_CAST",
    "RedundantVisibilityModifier",
    "NON_PUBLIC_PRIMARY_CONSTRUCTOR_OF_INLINE_CLASS",
    "UNSUPPORTED_FEATURE",
    "INVISIBLE_REFERENCE",
    "INVISIBLE_MEMBER"
)

package kotlin

import kotlin.internal.InlineOnly
import kotlin.jvm.JvmField

@SinceKotlin("1.3")
public inline class SuccessOrFailure<out T> @PublishedApi internal constructor(private val _value: Any?) {
    // discovery

    public val isSuccess: Boolean get() = _value !is Failure
    public val isFailure: Boolean get() = _value is Failure

    // value retrieval

    public fun getOrThrow(): T = when (_value) {
        is Failure -> throw _value.exception
        else -> _value as T
    }

    public fun getOrNull(): T? = when (_value) {
        is Failure -> null
        else -> _value as T
    }

    // exception retrieval

    public fun exceptionOrNull(): Throwable? = when (_value) {
        is Failure -> _value.exception
        else -> null
    }

    // internal API for inline functions

    @PublishedApi internal val exception: Throwable get() = (_value as Failure).exception
    @PublishedApi internal val value: T get() = _value as T

    // companion with constructors

    public companion object {
        @InlineOnly public inline fun <T> success(value: T): SuccessOrFailure<T> =
            SuccessOrFailure(value)

        @InlineOnly public inline fun <T> failure(exception: Throwable): SuccessOrFailure<T> =
            SuccessOrFailure<T>(Failure(exception))
    }
}

// top-Level internal failure-marker class
// todo: maybe move it to another kotlin.internal package?
@SinceKotlin("1.3")
@PublishedApi
internal class Failure @PublishedApi internal constructor(
    @JvmField
    val exception: Throwable
) : Serializable

@SinceKotlin("1.3")
@InlineOnly public inline fun <T> runOrCatch(block: () -> T): SuccessOrFailure<T> =
    try {
        SuccessOrFailure.success(block())
    } catch (e: Throwable) {
        SuccessOrFailure.failure(e)
    }

// -- extensions ---

@SinceKotlin("1.3")
@InlineOnly public inline fun <R, T : R> SuccessOrFailure<T>.getOrElse(default: () -> R): R = when {
    isFailure -> default()
    else -> value
}

// transformation

@SinceKotlin("1.3")
@InlineOnly public inline fun <U, T> SuccessOrFailure<T>.map(block: (T) -> U): SuccessOrFailure<U> =
    if (isFailure) this as SuccessOrFailure<U>
    else runOrCatch { block(value) }

@SinceKotlin("1.3")
@InlineOnly public inline fun <U, T: U> SuccessOrFailure<T>.handle(block: (Throwable) -> U): SuccessOrFailure<U> =
    if (isFailure) runOrCatch { block(exception) }
    else this

// "peek" onto value/exception and pipe

@SinceKotlin("1.3")
@InlineOnly public inline fun <T> SuccessOrFailure<T>.onFailure(block: (Throwable) -> Unit): SuccessOrFailure<T> {
    if (isFailure) block(exception)
    return this
}

@SinceKotlin("1.3")
@InlineOnly public inline fun <T> SuccessOrFailure<T>.onSuccess(block: (T) -> Unit): SuccessOrFailure<T> {
    if (isSuccess) block(value)
    return this
}

// -------------------