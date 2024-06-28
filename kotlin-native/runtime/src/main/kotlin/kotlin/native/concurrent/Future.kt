/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.native.concurrent

import kotlin.native.internal.Frozen

/**
 * State of the future object.
 */
@ObsoleteWorkersApi
public enum class FutureState(public val value: Int) {
    INVALID(0),
    /** Future is scheduled for execution. */
    SCHEDULED(1),
    /** Future result is computed. */
    COMPUTED(2),
    /** Future is cancelled. */
    CANCELLED(3),
    /** Computation thrown an exception. */
    THROWN(4)
}

/**
 * Class representing abstract computation, whose result may become available in the future.
 */
@Suppress("NON_PUBLIC_PRIMARY_CONSTRUCTOR_OF_INLINE_CLASS")
@ObsoleteWorkersApi
public value class Future<T> @PublishedApi internal constructor(public val id: Int) {
    /**
     * Blocks execution until the future is ready.
     *
     * @return the execution result of [code] consumed future's computaiton
     * @throws IllegalStateException if future is in [FutureState.INVALID], [FutureState.CANCELLED] or
     * [FutureState.THROWN] state
     */
    public inline fun <R> consume(code: (T) -> R): R = when (state) {
            FutureState.SCHEDULED, FutureState.COMPUTED -> {
                val value = @Suppress("UNCHECKED_CAST", "NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
                    (consumeFuture(id) as T)
                code(value)
            }
            FutureState.INVALID ->
                throw IllegalStateException("Future is in an invalid state")
            FutureState.CANCELLED -> {
                consumeFuture(id)
                throw IllegalStateException("Future is cancelled")
            }
            FutureState.THROWN -> {
                consumeFuture(id)
                throw IllegalStateException("Job has thrown an exception")
            }
        }

    /**
     * The result of the future computation.
     * Blocks execution until the future is ready. Second attempt to get will result in an error.
     */
    public val result: T
            get() = consume { it -> it }

    /**
     * A [FutureState] of this future
     */
    public val state: FutureState
        get() = FutureState.values()[stateOfFuture(id)]

    override public fun toString(): String = "future $id"
}


@Deprecated("Use 'waitForMultipleFutures' top-level function instead", ReplaceWith("waitForMultipleFutures(this, millis)"), DeprecationLevel.ERROR)
@ObsoleteWorkersApi
public fun <T> Collection<Future<T>>.waitForMultipleFutures(millis: Int): Set<Future<T>> = waitForMultipleFutures(this, millis)


/**
 * Wait for availability of futures in the collection. Returns set with all futures which have
 * value available for the consumption, i.e. [FutureState.COMPUTED].
 *
 * @param timeoutMillis the amount of time in milliseconds to wait for the computed future
 */
@ObsoleteWorkersApi
public fun <T> waitForMultipleFutures(futures: Collection<Future<T>>, timeoutMillis: Int): Set<Future<T>> {
    val result = mutableSetOf<Future<T>>()

    while (true) {
        val versionToken = versionToken()
        for (future in futures) {
            if (future.state == FutureState.COMPUTED) {
                result += future
            }
        }
        if (result.isNotEmpty()) return result

        if (waitForAnyFuture(versionToken, timeoutMillis)) break
    }

    for (future in futures) {
        if (future.state == FutureState.COMPUTED) {
            result += future
        }
    }

    return result
}
