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
 * Unique identifier of the worker. Workers can be used from other workers.
 */
typealias WorkerId = Int


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
            Future<Nothing?>(requestTerminationInternal(id, processScheduledJobs))

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
    @Suppress("UNUSED_PARAMETER")
    fun <T1, T2> schedule(mode: TransferMode, producer: () -> T1, @VolatileLambda job: (T1) -> T2): Future<T2> =
            /**
             * This function is a magical operation, handled by lowering in the compiler, and replaced with call to
             *   scheduleImpl(worker, mode, producer, job)
             * but first ensuring that `job` parameter  doesn't capture any state.
             */
            throw RuntimeException("Shall not be called directly")

    override fun equals(other: Any?) = (other is Worker) && (id == other.id)

    override fun hashCode() = id
}

/**
 * Start new scheduling primitive, such as thread, to accept new tasks via `schedule` interface.
 * Typically new worker may be needed for computations offload to another core, for IO it may be
 * better to use non-blocking IO combined with more lightweight coroutines.
 */
fun startWorker(): Worker = Worker(startInternal())

// Private APIs.
@konan.internal.ExportForCompiler
internal fun scheduleImpl(worker: Worker, mode: TransferMode, producer: () -> Any?,
                          job: CPointer<CFunction<*>>): Future<Any?> =
        Future<Any?>(scheduleInternal(worker.id, mode.value, producer, job))

@SymbolName("Kotlin_Worker_startInternal")
external internal fun startInternal(): WorkerId

@SymbolName("Kotlin_Worker_requestTerminationWorkerInternal")
external internal fun requestTerminationInternal(id: WorkerId, processScheduledJobs: Boolean): FutureId

@SymbolName("Kotlin_Worker_scheduleInternal")
external internal fun scheduleInternal(
        id: WorkerId, mode: Int, producer: () -> Any?, job: CPointer<CFunction<*>>): FutureId

@ExportForCppRuntime
internal fun ThrowWorkerUnsupported(): Unit =
        throw UnsupportedOperationException("Workers are not supported")

@ExportForCppRuntime
internal fun ThrowWorkerInvalidState(): Unit =
        throw IllegalStateException("Illegal transfer state")

@ExportForCppRuntime
internal fun WorkerLaunchpad(function: () -> Any?) = function()
