/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.native.concurrent

import kotlin.native.internal.ExportForCppRuntime
import kotlin.native.internal.Frozen
import kotlin.native.internal.VolatileLambda
import kotlin.native.internal.IntrinsicType
import kotlin.native.internal.TypedIntrinsic
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
@Suppress("NON_PUBLIC_PRIMARY_CONSTRUCTOR_OF_INLINE_CLASS")
public inline class Worker @PublishedApi internal constructor(val id: Int) {
    companion object {
        /**
         * Start new scheduling primitive, such as thread, to accept new tasks via `execute` interface.
         * Typically new worker may be needed for computations offload to another core, for IO it may be
         * better to use non-blocking IO combined with more lightweight coroutines.
         *
         * @param errorReporting controls if an uncaught exceptions in the worker will be printed out
         */
        public fun start(errorReporting: Boolean = true): Worker = Worker(startInternal(errorReporting))

        /**
         * Return the current worker. Worker context is accessible to any valid Kotlin context,
         * but only actual active worker produced with [Worker.start] automatically processes execution requests.
         * For other situations [processQueue] must be called explicitly to process request queue.
         */
        public val current: Worker get() = Worker(currentInternal())

        /**
         * Create worker object from a C pointer.
         *
         *  @param pointer value returned earlier by [Worker.asCPointer]
         */
        public fun fromCPointer(pointer: COpaquePointer?) =
                if (pointer != null) Worker(pointer.toLong().toInt()) else throw IllegalArgumentException()
    }

    /**
     * Requests termination of the worker.
     *
     * @param processScheduledJobs controls is we shall wait until all scheduled jobs processed,
     * or terminate immediately. If there are jobs to be execucted with [executeAfter] their execution
     * is awaited for.
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
     * Note, that some technically disjoint subgraphs may lead to `kotlin.IllegalStateException`
     * so `kotlin.native.internal.GC.collect()` could be called in the end of `producer` and `job`
     * if garbage cyclic structures or other uncollected objects refer to the value being transferred.
     *
     * @return the future with the computation result of [job]
     */
    @Suppress("UNUSED_PARAMETER")
    @TypedIntrinsic(IntrinsicType.WORKER_EXECUTE)
    public fun <T1, T2> execute(mode: TransferMode, producer: () -> T1, @VolatileLambda job: (T1) -> T2): Future<T2> =
            /*
             * This function is a magical operation, handled by lowering in the compiler, and replaced with call to
             *   executeImpl(worker, mode, producer, job)
             * but first ensuring that `job` parameter  doesn't capture any state.
             */
            throw RuntimeException("Shall not be called directly")

    /**
     * Plan job for further execution in the worker. [operation] parameter must be either frozen, or execution to be
     * planned on the current worker. Otherwise [IllegalStateException] will be thrown.
     * [afterMicroseconds] defines after how many microseconds delay execution shall happen, 0 means immediately,
     * on negative values [IllegalArgumentException] is thrown.
     */
    public fun executeAfter(afterMicroseconds: Long = 0, operation: () -> Unit): Unit {
        val current = currentInternal()
        if (current != id && !operation.isFrozen) throw IllegalStateException("Job for another worker must be frozen")
        if (afterMicroseconds < 0) throw IllegalArgumentException("Timeout parameter must be non-negative")
        executeAfterInternal(id, operation, afterMicroseconds)
    }

    /**
     * Process pending job(s) on the queue of this worker, returns `true` if something was processed
     * and `false` otherwise. Note that jobs scheduled with [executeAfter] using non-zero timeout are
     * not processed this way. If termination request arrives while processing the queue via this API,
     * worker is marked as terminated and will exit once the current request is done with.
     */
    public fun processQueue(): Boolean = processQueueInternal(id)

    /**
     * String representation of this worker.
     */
    override public fun toString(): String = "worker $id"

    /**
     * Convert worker to a COpaquePointer value that could be passed via native void* pointer.
     * Can be used as an argument of [Worker.fromCPointer].
     */
    public fun asCPointer() : COpaquePointer? = id.toLong().toCPointer()
}