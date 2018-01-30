/*
 * Copyright 2010-2018 JetBrains s.r.o.
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

package konan.worker

import konan.SymbolName
import konan.internal.ExportForCppRuntime

/**
 * Unique identifier of the future. Futures can be used from other workers.
 */
typealias FutureId = Int

/**
 * State of the future object.
 */
enum class FutureState(val value: Int) {
    INVALID(0),
    // Future is scheduled for execution.
    SCHEDULED(1),
    // Future result is computed.
    COMPUTED(2),
    // Future is cancelled.
    CANCELLED(3)
}

/**
 * Class representing abstract computation, whose result may become available in the future.
 */
// TODO: make me value class!
class Future<T> internal constructor(val id: FutureId) {
    /**
     * Blocks execution until the future is ready.
     */
    inline fun <R> consume(code: (T) -> R) =
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

    fun result(): T  = consume { it -> it }

    val state: FutureState
        get() = FutureState.values()[stateOfFuture(id)]

    override fun equals(other: Any?) = (other is Future<*>) && (id == other.id)

    override fun hashCode() = id
}

/**
 * Wait for availability of futures in the collection. Returns set with all futures which have
 * value available for the consumption.
 */
fun <T> Collection<Future<T>>.waitForMultipleFutures(millis: Int): Set<Future<T>> {
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

// Private APIs.
@SymbolName("Kotlin_Worker_stateOfFuture")
external internal fun stateOfFuture(id: FutureId): Int

@SymbolName("Kotlin_Worker_consumeFuture")
@kotlin.internal.InlineExposed
external internal fun consumeFuture(id: FutureId): Any?

@SymbolName("Kotlin_Worker_waitForAnyFuture")
external internal fun waitForAnyFuture(versionToken: Int, millis: Int): Boolean

@SymbolName("Kotlin_Worker_versionToken")
external internal fun versionToken(): Int

