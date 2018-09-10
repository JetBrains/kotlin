/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.native.concurrent

import kotlin.native.internal.ExportForCppRuntime
import kotlin.native.internal.Frozen
import kotlin.native.internal.VolatileLambda
import kotlinx.cinterop.*

/**
 *  ## Workers: theory of operations.
 *
 * [Worker] represents asynchronous and concurrent computation, usually performed by other threads
 * in the same process. Object passing between workers is performed using transfer operation, so that
 * object graph belongs to one worker at the time, but can be disconnected and reconnected as needed.
 * See 'Object Transfer Basics' and [TransferMode] for more details on how objects shall be transferred.
 * This approach ensures that no concurrent access happens to same object, while data may flow between
 * workers as needed.
 */

/**
 * Class representing worker.
 */
@Frozen
public class Worker private constructor(val id: Int) {
    companion object {
        /**
         * Start new scheduling primitive, such as thread, to accept new tasks via `execute` interface.
         * Typically new worker may be needed for computations offload to another core, for IO it may be
         * better to use non-blocking IO combined with more lightweight coroutines.
         */
        public fun start(): Worker = Worker(startInternal())
    }

    /**
     * Requests termination of the worker.
     *
     * @param processScheduledJobs controls is we shall wait until all scheduled jobs processed,
     * or terminate immediately.
     */
    public fun requestTermination(processScheduledJobs: Boolean = true) =
            Future<Unit>(requestTerminationInternal(id, processScheduledJobs))

    /**
     * Plan job for further execution in the worker. Execute is a two-phase operation:
     * - first [producer] function is executed, and resulting object and whatever it refers to
     * is analyzed for being an isolated object subgraph, if in checked mode.
     * - Afterwards, this disconnected object graph and [job] function pointer is being added to jobs queue
     * of the selected worker. Note that [job] must not capture any state itself, so that whole state is
     * explicitly stored in object produced by [producer]. Scheduled job is being executed by the worker,
     * and result of such a execution is being disconnected from worker's object graph. Whoever will consume
     * the future, can use result of worker's computations.
     *
     * @return the future with the computation result of [job]
     */
    @Suppress("UNUSED_PARAMETER")
    public fun <T1, T2> execute(mode: TransferMode, producer: () -> T1, @VolatileLambda job: (T1) -> T2): Future<T2> =
            /*
             * This function is a magical operation, handled by lowering in the compiler, and replaced with call to
             *   executeImpl(worker, mode, producer, job)
             * but first ensuring that `job` parameter  doesn't capture any state.
             */
            throw RuntimeException("Shall not be called directly")

    override public fun equals(other: Any?): Boolean = (other is Worker) && (id == other.id)

    override public fun hashCode(): Int = id

    override public fun toString(): String = "worker $id"
}