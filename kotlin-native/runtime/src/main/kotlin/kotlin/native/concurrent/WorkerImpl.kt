/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalTime::class, ExperimentalForeignApi::class, ExperimentalNativeApi::class, BetaInteropApi::class)

package kotlin.native.concurrent

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.autoreleasepool
import kotlin.concurrent.Volatile
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.internal.InternalForKotlinNative
import kotlin.native.internal.collections.PriorityQueue
import kotlin.native.internal.ReportUnhandledException
import kotlin.native.internal.ThrowIllegalStateException
import kotlin.native.internal.concurrent.Monitor
import kotlin.native.internal.concurrent.Synchronizable
import kotlin.native.internal.concurrent.currentThreadId
import kotlin.native.internal.concurrent.startThread
import kotlin.native.ref.WeakReference
import kotlin.native.ref.createCleaner
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.ComparableTimeMark
import kotlin.time.Duration.Companion.microseconds
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

@ObsoleteWorkersApi
@ThreadLocal
private var thisThreadWorker: WorkerImpl? = null

@ObsoleteWorkersApi
private fun thisThreadWorkerEnsureInitialized(): WorkerImpl {
    return thisThreadWorker ?: run {
        val worker = theState.addWorker(WorkerExceptionHandling.DEFAULT, null, WorkerKind.OTHER)
        thisThreadWorker = worker
        worker
    }
}

@ObsoleteWorkersApi
private fun MutableList<Job>.cancelAll() {
    for (job in this) {
        when (job) {
            is Job.Regular -> {
                job.future.cancel()
            }
            is Job.TerminationRequest -> {
                // TODO (inherited from c++ impl): any more processing here?
                job.future.cancel()
            }
            is Job.ExecuteAfter -> {}
        }
    }
    clear()
}


private enum class WorkerKind {
    NATIVE,  // Workers created using Worker.start public API.
    OTHER,   // Any other kind of workers.
}

private enum class WorkerExceptionHandling {
    DEFAULT, IGNORE
}

@ObsoleteWorkersApi
private class WorkerImpl(
        val id: Int,
        private val exceptionHandling: WorkerExceptionHandling,
        val name: String?,
        val kind: WorkerKind
) : Synchronizable() {
    private val queue = ArrayDeque<Job>()
    private val delayedJobs = PriorityQueue<Job.ExecuteAfter> { l, r -> l.whenExecute!!.compareTo(r.whenExecute!!) }

    @Suppress("unused")
    private val queueCleaner = createCleaner(queue) { it.cancelAll() }

    var terminated = false

    @Volatile
    var threadId: ULong? = null
        private set

    @Suppress("ControlFlowWithEmptyBody")
    fun startEventLoop() {
        startThread {
            threadId = currentThreadId()
            require(thisThreadWorker == null)
            thisThreadWorker = this

            while (processQueueElement(true) != JobKind.TERMINATE) {
            }

            // Someone might be waiting on our futures - release them earlier than our cleaner would
            queue.cancelAll()

            theState.onWorkerTermination()
        }
    }

    fun putJob(job: Job, toFront: Boolean) {
        synchronized {
            if (toFront) {
                queue.addFirst(job)
            } else {
                queue.addLast(job)
            }
            notify()
        }
    }

    fun putDelayedJob(job: Job.ExecuteAfter) {
        synchronized {
            delayedJobs.add(job)
            notify()
        }
    }

    fun waitDelayed(blocking: Boolean): Boolean {
        synchronized {
            if (delayedJobs.isEmpty()) return false
            if (blocking) waitForQueue(null)
            return true
        }
    }

    fun getJob(blocking: Boolean): Job? {
        synchronized {
            assert(!terminated)
            if (queue.isEmpty() && !blocking) {
                return null
            }
            waitForQueue(null)
            val result = queue.removeFirst()
            return result
        }
    }

    @IgnorableReturnValue
    fun Monitor.MonitoredSection.waitForQueue(until: ComparableTimeMark?): Boolean {
        fun <T : Comparable<T>> minOf(first: T?, second: T?): T? {
            if (first == null) return second
            if (second == null) return first

            return kotlin.comparisons.minOf(first, second)
        }

        while (queue.isEmpty()) {
            val nextDelayed = delayedJobs.firstOrNull()
            if (nextDelayed?.whenExecute?.hasPassedNow() == true) {
                delayedJobs.remove(nextDelayed)
                queue.addLast(nextDelayed)
                return true
            }
            val minWait = minOf(nextDelayed?.whenExecute, until)
            if (minWait != null) {
                // need to wait some finite time
                // on the next iteration there is either a delayed job ready or we are done
                waitUntil(minWait)
            } else {
                wait()
            }
            if (until != null) return queue.isNotEmpty()
        }
        return true
    }

    fun processQueueElement(blocking: Boolean): JobKind {
        if (terminated) return JobKind.TERMINATE
        val job = getJob(blocking)
        when (job) {
            null -> return JobKind.NONE
            is Job.TerminationRequest -> {
                if (job.waitDelayed && waitDelayed(blocking)) {
                    putJob(job, false)
                    return JobKind.NONE
                }
                queue.cancelAll()
                terminated = true
                // Termination request, remove the worker and notify the future.
                theState.removeWorker(id)
                job.future.storeResult(null, true)
            }
            is Job.ExecuteAfter -> {
                try {
                    autoreleasepool {
                        job.operation()
                    }
                } catch (e: Throwable) {
                    if (exceptionHandling == WorkerExceptionHandling.DEFAULT) {
                        processUnhandledException(e)
                    }
                }
            }
            is Job.Regular -> {
                var ok = true
                val result = try {
                    autoreleasepool {
                        job.job(job.argument)
                    }
                } catch (e: Throwable) {
                    ok = false
                    if (exceptionHandling == WorkerExceptionHandling.DEFAULT) {
                        // TODO (inherited from c++ impl): Pass exception object into the future and do nothing in the default case.
                        ReportUnhandledException(e)
                    }
                    null
                }
                // Notify the future.
                job.future.storeResult(result, ok)
            }
        }
        return job.kind
    }

    fun park(timeout: Duration, process: Boolean): Boolean {
        synchronized {
            if (terminated) {
                return false
            }
            val until = TimeSource.Monotonic.markNow() + timeout
            var arrived: Boolean
            do {
                arrived = waitForQueue(until)
            } while (until.hasNotPassedNow() && !arrived)
            if (!process) {
                return arrived
            }
            if (!arrived) {
                return false
            }
        }
        return processQueueElement(false) >= JobKind.REGULAR
    }
}

@ObsoleteWorkersApi
private class FutureImpl(val id: Int) : Synchronizable() {
    var state: FutureState = FutureState.SCHEDULED
    var result: Any? = null

    fun consumeResult(): Any? {
        synchronized {
            while (state == FutureState.SCHEDULED) {
                wait()
            }
            if (state == FutureState.THROWN) {
                ThrowIllegalStateException()
            }
            return result.also { result = null }
        }
    }

    fun storeResult(result: Any?, ok: Boolean) {
        synchronized {
            state = if (ok) FutureState.COMPUTED else FutureState.THROWN
            this@FutureImpl.result = result
            notifyAll()
        }
        theState.signalAnyFuture()
    }

    fun cancel() {
        synchronized {
            state = FutureState.CANCELLED
            this@FutureImpl.result = null
            notifyAll()
        }
        theState.signalAnyFuture()
    }


    fun state(): FutureState {
        synchronized {
            return state
        }
    }

}

@ObsoleteWorkersApi
private class StateImpl : Synchronizable() {
    private var nextWorkerId = 1
    private var nextFutureId = 1
    private var currentVersion = 0

    // Futures are owned by the state
    private val futures = mutableMapOf<Int, FutureImpl?>()

    // Workers are owned by their threads
    private val workers = mutableMapOf<Int, WeakReference<WorkerImpl>>()

    fun addWorker(exceptionHandling: WorkerExceptionHandling, name: String?, kind: WorkerKind): WorkerImpl {
        synchronized {
            val worker = WorkerImpl(nextWorkerId++, exceptionHandling, name, kind)
            workers[worker.id] = WeakReference(worker)
            return worker
        }
    }

    fun removeWorker(id: Int) {
        synchronized {
            workers.remove(id)
        }
    }

    /**
     * @param jobFunction The function to be executed. If `null`, adds a termination request job.
     */
    fun addJobToWorker(workerId: Int, jobFunction: ExecuteJob?, jobArgument: Any?, toFront: Boolean): FutureImpl? {
        synchronized {
            val worker = workers[workerId]?.value ?: return null
            val future = FutureImpl(nextFutureId++)
            futures[future.id] = future

            val job = if (jobFunction == null) {
                Job.TerminationRequest(future, !toFront)
            } else {
                Job.Regular(jobFunction, jobArgument, future)
            }

            worker.putJob(job, toFront)

            return future
        }
    }

    fun executeJobAfterInWorker(workerId: Int, operation: () -> Unit, after: Duration): Boolean {
        synchronized {
            assert(after.isPositive())

            val worker = workers[workerId]?.value ?: return false
            if (after == Duration.ZERO) {
                worker.putJob(Job.ExecuteAfter(operation, null), false)
            } else {
                worker.putDelayedJob(Job.ExecuteAfter(operation, TimeSource.Monotonic.markNow() + after))
            }
            return true
        }
    }

    // Returns `true` if something was indeed processed.
    fun processQueue(workerId: Int): Boolean {
        // Can only process queue of the current worker.
        val worker = thisThreadWorkerEnsureInitialized()
        if (workerId != worker.id) ThrowWrongWorkerOrAlreadyTerminated()
        val kind = worker.processQueueElement(false)
        return kind != JobKind.NONE && kind != JobKind.TERMINATE
    }

    fun park(workerId: Int, timeout: Duration, process: Boolean): Boolean {
        // Can only park current worker.
        val worker = thisThreadWorkerEnsureInitialized()
        if (workerId != worker.id) ThrowWrongWorkerOrAlreadyTerminated()
        return worker.park(timeout, process)
    }

    fun stateOfFuture(futureId: Int): FutureState {
        synchronized {
            return futures[futureId]?.state() ?: FutureState.INVALID
        }
    }

    fun consumeFuture(futureId: Int): Any? {
        val future = synchronized {
            futures[futureId] ?: run {
                // Caller checks `stateOfFuture` first, so this code is reachable
                // only when trying to consume future twice concurrently.
                ThrowFutureInvalidState()
            }
        }

        val result = future.consumeResult()

        synchronized {
            futures[futureId] = null
        }

        return result
    }

    fun getWorkerName(workerId: Int): String? {
        synchronized {
            val worker = workers[workerId]?.value ?: ThrowWorkerAlreadyTerminated()
            return worker.name
        }
    }

    fun waitForAnyFuture(version: Int, timeout: Duration?): Boolean {
        synchronized {
            if (version != currentVersion) return false

            if (timeout == null) {
                wait()
            } else {
                wait(timeout)
            }
            return true
        }
    }

    fun signalAnyFuture() {
        synchronized {
            currentVersion++
            notifyAll()
        }
    }

    fun versionToken(): Int = synchronized { currentVersion }

    fun getWorkerPlatformThreadId(workerId: Int): ULong {
        synchronized {
            return workers[workerId]?.value?.threadId ?: ThrowWorkerAlreadyTerminated()
        }
    }

    fun getActiveWorkers(): IntArray {
        synchronized {
            return workers.values.mapNotNull { it.value?.id }.toIntArray()
        }
    }

    fun waitWorkerTermination(workerId: Int) {
        synchronized {
            val worker = workers[workerId]?.value ?: return
            while (!worker.terminated) wait()
        }
    }

    fun onWorkerTermination() {
        synchronized {
            notifyAll()
        }
    }
}

@ObsoleteWorkersApi
private val theState = StateImpl()

private enum class JobKind {
    NONE,
    TERMINATE,

    // Order is important in sense that all job kinds after this one is considered
    // processed for APIs returning request process status.
    REGULAR,
    EXECUTE_AFTER,
}

private typealias ExecuteJob = (Any?) -> Any?

@ObsoleteWorkersApi
private sealed class Job(val kind: JobKind) {
    class Regular(val job: ExecuteJob, val argument: Any?, val future: FutureImpl) : Job(JobKind.REGULAR)
    class TerminationRequest(val future: FutureImpl, val waitDelayed: Boolean) : Job(JobKind.TERMINATE)
    class ExecuteAfter(val operation: () -> Unit, val whenExecute: ComparableTimeMark?) : Job(JobKind.EXECUTE_AFTER)
}

@InternalForKotlinNative
@ObsoleteWorkersApi
public fun waitWorkerTermination(worker: Worker) {
    theState.waitWorkerTermination(worker.id)
}

@ObsoleteWorkersApi
internal fun startInternal(errorReporting: Boolean, name: String?): Int {
    val worker = theState.addWorker(
            if (errorReporting) WorkerExceptionHandling.DEFAULT else WorkerExceptionHandling.IGNORE,
            name,
            WorkerKind.NATIVE
    )
    worker.startEventLoop()
    return worker.id
}

@ObsoleteWorkersApi
internal fun currentInternal(): Int = thisThreadWorkerEnsureInitialized().id

@ObsoleteWorkersApi
internal fun requestTerminationInternal(id: Int, processScheduledJobs: Boolean): Int {
    val future = theState.addJobToWorker(
            id,
            null,
            null,
            !processScheduledJobs
    ) ?: ThrowWorkerAlreadyTerminated()
    return future.id
}

@ObsoleteWorkersApi
internal fun executeInternal(id: Int, jobArgument: Any?, job: ExecuteJob): Int {
    val future = theState.addJobToWorker(id, job, jobArgument, false)
    return future?.id ?: ThrowWorkerAlreadyTerminated()
}

@ObsoleteWorkersApi
internal fun executeAfterInternal(id: Int, operation: () -> Unit, afterMicroseconds: Long) {
    val scheduled = theState.executeJobAfterInWorker(id, operation, afterMicroseconds.microseconds)
    if (!scheduled) ThrowWorkerAlreadyTerminated()
}

@ObsoleteWorkersApi
internal fun processQueueInternal(id: Int): Boolean {
    return theState.processQueue(id)
}

@ObsoleteWorkersApi
internal fun parkInternal(id: Int, timeoutMicroseconds: Long, process: Boolean): Boolean {
    return theState.park(id, timeoutMicroseconds.microseconds, process)
}

@ObsoleteWorkersApi
internal fun getWorkerNameInternal(id: Int) = theState.getWorkerName(id)


@ObsoleteWorkersApi
internal fun getPlatformThreadIdInternal(id: Int): ULong = theState.getWorkerPlatformThreadId(id)

@ObsoleteWorkersApi
internal fun getActiveWorkersInternal(): IntArray = theState.getActiveWorkers()

@PublishedApi
@ObsoleteWorkersApi
internal fun consumeFuture(id: Int): Any? = theState.consumeFuture(id)

@ObsoleteWorkersApi
internal fun waitForAnyFuture(versionToken: Int, millis: Int): Boolean = theState.waitForAnyFuture(versionToken, millis.toLong().milliseconds)

@ObsoleteWorkersApi
internal fun versionToken(): Int = theState.versionToken()

@ObsoleteWorkersApi
internal fun stateOfFuture(id: Int): Int = theState.stateOfFuture(id).ordinal

private fun ThrowWorkerAlreadyTerminated(): Nothing =
        throw IllegalStateException("Worker is already terminated")

private fun ThrowWrongWorkerOrAlreadyTerminated(): Nothing =
        throw IllegalStateException("Worker is not current or already terminated")

private fun ThrowFutureInvalidState(): Nothing =
        throw IllegalStateException("Future is in an invalid state")
