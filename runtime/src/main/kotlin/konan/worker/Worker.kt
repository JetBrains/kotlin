/*
 * Copyright 2010-2017 JetBrains s.r.o.
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
import kotlinx.cinterop.*

/**
 *      Workers: theory of operations.
 *
 *  Worker represent asynchronous and concurrent computation, usually performed by other threads
 * in the same process. Object passing between workers is performed using transfer operation, so that
 * object graph belongs to one worker at the time, but can be disconnected and reconnected as needed.
 * See 'Object Transfer Basics' below for more details on how objects shall be transferred.
 * This approach ensures that no concurrent access happens to same object, while data may flow between
 * workers as needed.
 */

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
 *  Object Transfer Basics.
 *
 *  Objects can be passed between threads in one of two possible modes.
 *
 *    - CHECKED - object subgraph is checked to be not reachable by other globals or locals, and passed
 *      if so, otherwise an exception is thrown
 *    - UNCHECKED - object is blindly passed to another worker, if there are references
 *      left in the passing worker - it may lead to crash or program malfunction
 *
 *   Checked mode checks if object is no longer used in passing worker, using memory-management
 *  specific algorithm (ARC implementation relies on trial deletion on object graph rooted in
 *  passed object), and throws IllegalStateException if object graph rooted in transferred object
 *  is reachable by some other means,
 *
 *   Unchecked mode, intended for most performance crititcal operations, where object graph ownership
 *  is expected to be correct (such as application debugged earlier in CHECKED mode), just transfers
 *  ownership without further checks.
 *
 *   Note, that for some cases cycle collection need to be done to ensure that dead cycles do not affect
 *  reachability of passed object graph. See `konan.internal.GC.collect()`.
 *
 */
enum class TransferMode(val value: Int) {
    CHECKED(0),
    UNCHECKED(1) // USE UNCHECKED MODE ONLY IF ABSOLUTELY SURE WHAT YOU'RE DOING!!!
}

/**
 * Unique identifier of the worker. Workers can be used from other workers.
 */
typealias WorkerId = Int
/**
 * Unique identifier of the future. Futures can be used from other workers.
 */
typealias FutureId = Int

/**
 * Class representing abstract computation, whose result may become available in the future.
 */
// TODO: make me value class!
class Future<T> internal constructor(val id: FutureId) {
    /**
     * Blocks execution until the future is ready.
     */
    fun consume(code: (T) -> Unit) {
        when (state) {
            FutureState.SCHEDULED, FutureState.COMPUTED -> {
                val value = consumeFuture(id) as T
                code(value)
            }
            FutureState.INVALID ->
                throw IllegalStateException("Future is in an invalid state: $state")
            FutureState.CANCELLED ->
                throw IllegalStateException("Future is cancelled")
        }
    }

    val state: FutureState
            get() = FutureState.values()[stateOfFuture(id)]

    override fun equals(other: Any?): Boolean {
        return (other is Future<*>) && (id == other.id)
    }

    override fun hashCode(): Int {
        return id
    }
}

/**
 * Class representing worker.
 */
// TODO: make me value class!
class Worker(val id: WorkerId) {
    /**
     * Requests termination of the worker. `processScheduledJobs` controls is we shall wait
     * until all scheduled jobs processed, or terminate immediately.
     */
    fun requestTermination(processScheduledJobs: Boolean = true) =
            Future<Any?>(requestTerminationInternal(id, processScheduledJobs))

    /**
     * Schedule a job for further execution in the worker. Schedule is a two-phase operation,
     * first `producer` function is executed, and resulting object and whatever it refers to
     * is analyzed for being an isolated object subgraph, if in checked mode.
     * Afterwards, this disconnected object graph and `job` function pointer is being added to jobs queue
     * of the selected worker. Note that `job` must not capture any state itself, so that whole state is
     * explicitly stored in object produced by `producer`. Scheduled job is being executed by the worker,
     * and result of such a execution is being disconnected from worker's object graph. Whoever will consume
     * the future, can use result of worker's computations.
     */
    fun <T1, T2> schedule(mode: TransferMode, producer: () -> T1,
                          @VolatileLambda job: (T1) -> T2): Future<T2> =
            /**
             * This function is a magical operation, handled by lowering in the compiler, and replaced with call to
             *   scheduleImpl(worker, mode, producer, job)
             * but first ensuring that `job` parameter  doesn't capture any state.
             */
            throw RuntimeException("Shall not be called directly")

    override fun equals(other: Any?): Boolean {
        return (other is Worker) && (id == other.id)
    }

    override fun hashCode(): Int {
        return id
    }
}

/**
 * Start new scheduling primitive, such as thread, to accept new tasks via `schedule` interface.
 * Typically new worker may be needed for computations offload to another core, for IO it may be
 * better to use non-blocking IO combined with more lightweight coroutines.
 */
fun startWorker() : Worker = Worker(startInternal())

/**
 * Wait for availability of futures in the collection. Returns set with all futures which have
 * value available for the consumption.
 */
fun <T> Collection<Future<T>>.waitForMultipleFutures(millis: Int) : Set<Future<T>> {
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

/**
 * Creates verbatim *shallow* copy of passed object, use carefully to create disjoint object graph.
 */
fun <T> T.shallowCopy(): T = shallowCopyInternal(this) as T

/**
 * Creates verbatim *deep* copy of passed object's graph, use *VERY* carefully to create disjoint object graph.
 * Note that this function could potentially duplicate a lot of objects.
 */
fun <T> T.deepCopy(): T = TODO()

// Implementation details.
@konan.internal.ExportForCompiler
internal fun scheduleImpl(worker: Worker, mode: TransferMode, producer: () -> Any?,
                          job: CPointer<CFunction<*>>) : Future<Any?> =
        Future<Any?>(scheduleInternal(worker.id, mode.value, producer, job))

@SymbolName("Kotlin_Worker_startInternal")
external internal fun startInternal() : WorkerId

@SymbolName("Kotlin_Worker_requestTerminationWorkerInternal")
external internal fun requestTerminationInternal(id: WorkerId, processScheduledJobs: Boolean): FutureId

@SymbolName("Kotlin_Worker_scheduleInternal")
external internal fun scheduleInternal(
        id: WorkerId, mode: Int, producer: () -> Any?, job: CPointer<CFunction<*>>) : FutureId

@SymbolName("Kotlin_Worker_shallowCopyInternal")
external internal fun shallowCopyInternal(value: Any?) : Any?

@SymbolName("Kotlin_Worker_stateOfFuture")
external internal fun stateOfFuture(id: FutureId): Int

@SymbolName("Kotlin_Worker_consumeFuture")
external internal fun consumeFuture(id: FutureId): Any?

@SymbolName("Kotlin_Worker_waitForAnyFuture")
external internal fun waitForAnyFuture(versionToken: Int, millis: Int): Boolean

@SymbolName("Kotlin_Worker_versionToken")
external internal fun versionToken(): Int

@ExportForCppRuntime
internal fun ThrowWorkerUnsupported(): Unit =
        throw UnsupportedOperationException("Workers are not supported")

@ExportForCppRuntime
internal fun ThrowWorkerInvalidState(): Unit =
        throw IllegalStateException("Illegal transfer state")

@ExportForCppRuntime
internal fun WorkerLaunchpad(function: () -> Any?) = function()
