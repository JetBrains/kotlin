/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.native.concurrent

import kotlin.native.internal.Frozen

/**
 * State of the future object.
 */
enum class FutureState(val value: Int) {
    INVALID(0),
    /** Future is scheduled for execution. */
    SCHEDULED(1),
    /** Future result is computed. */
    COMPUTED(2),
    /** Future is cancelled. */
    CANCELLED(3)
}

/**
 * Class representing abstract computation, whose result may become available in the future.
 */
@Frozen
public class Future<T> internal constructor(val id: Int) {
    /**
     * Blocks execution until the future is ready.
     *
     * @return the execution result of [code] consumed futures's computaiton
     * @throws IllegalStateException if current future has [FutureState.INVALID] or [FutureState.CANCELLED] state
     */
    public inline fun <R> consume(code: (T) -> R): R =
            when (state) {
                FutureState.SCHEDULED, FutureState.COMPUTED -> {
                    val value = @Suppress("UNCHECKED_CAST", "NON_PUBLIC_CALL_FROM_PUBLIC_INLINE") (consumeFuture(id) as T)
                    code(value)
                }
                FutureState.INVALID ->
                    throw IllegalStateException("Future is in an invalid state: $state")
                FutureState.CANCELLED ->
                    throw IllegalStateException("Future is cancelled")
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

    public override fun equals(other: Any?): Boolean = (other is Future<*>) && (id == other.id)

    public override fun hashCode(): Int = id

    override public fun toString(): String = "future $id"
}

/**
 * Wait for availability of futures in the collection. Returns set with all futures which have
 * value available for the consumption, i.e. [FutureState.COMPUTED]
 *
 * @param millis the amount of time to wait for the computed future
 */
public fun <T> Collection<Future<T>>.waitForMultipleFutures(millis: Int): Set<Future<T>> {
    val result = mutableSetOf<Future<T>>()

    while (true) {
        val versionToken = versionToken()
        for (future in this) {
            if (future.state == FutureState.COMPUTED) {
                result += future
            }
        }
        if (result.isNotEmpty()) return result

        if (waitForAnyFuture(versionToken, millis)) break
    }

    for (future in this) {
        if (future.state == FutureState.COMPUTED) {
            result += future
        }
    }

    return result
}