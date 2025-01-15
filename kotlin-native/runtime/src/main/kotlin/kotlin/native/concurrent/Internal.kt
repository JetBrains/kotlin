/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
@file:OptIn(ExperimentalForeignApi::class)
package kotlin.native.concurrent

import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.identityHashCode
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CFunction
import kotlinx.cinterop.ExperimentalForeignApi
import kotlin.native.internal.*
import kotlin.native.internal.ref.*

// Implementation details.

@GCUnsafeCall("Kotlin_Worker_stateOfFuture")
@ObsoleteWorkersApi
external internal fun stateOfFuture(id: Int): Int

@PublishedApi
@ObsoleteWorkersApi
internal fun consumeFuture(id: Int): Any? {
    val ref = consumeFutureInternal(id)
    val result = dereferenceExternalRCRef(ref)
    releaseExternalRCRef(ref)
    disposeExternalRCRef(ref)
    return result
}

@GCUnsafeCall("Kotlin_Worker_consumeFuture")
@ObsoleteWorkersApi
external private fun consumeFutureInternal(id: Int): ExternalRCRef

@GCUnsafeCall("Kotlin_Worker_waitForAnyFuture")
@ObsoleteWorkersApi
external internal fun waitForAnyFuture(versionToken: Int, millis: Int): Boolean

@GCUnsafeCall("Kotlin_Worker_versionToken")
@ObsoleteWorkersApi
external internal fun versionToken(): Int

@ExportForCompiler
@ObsoleteWorkersApi
internal fun executeImpl(worker: Worker, mode: TransferMode, producer: () -> Any?,
                         job: CPointer<CFunction<*>>): Future<Any?> {
    val jobArgument = createRetainedExternalRCRef(producer())
    return Future<Any?>(executeInternal(worker.id, mode.value, jobArgument, job))
}

@GCUnsafeCall("Kotlin_Worker_startInternal")
@ObsoleteWorkersApi
external internal fun startInternal(errorReporting: Boolean, name: ExternalRCRef): Int

@GCUnsafeCall("Kotlin_Worker_currentInternal")
@ObsoleteWorkersApi
external internal fun currentInternal(): Int

@GCUnsafeCall("Kotlin_Worker_requestTerminationWorkerInternal")
@ObsoleteWorkersApi
external internal fun requestTerminationInternal(id: Int, processScheduledJobs: Boolean): Int

@GCUnsafeCall("Kotlin_Worker_executeInternal")
@ObsoleteWorkersApi
external private fun executeInternal(
        id: Int, mode: Int, jobArgument: ExternalRCRef, job: CPointer<CFunction<*>>): Int

@GCUnsafeCall("Kotlin_Worker_executeAfterInternal")
@ObsoleteWorkersApi
external internal fun executeAfterInternal(id: Int, operation: ExternalRCRef, afterMicroseconds: Long): Unit

@GCUnsafeCall("Kotlin_Worker_processQueueInternal")
@ObsoleteWorkersApi
external internal fun processQueueInternal(id: Int): Boolean

@GCUnsafeCall("Kotlin_Worker_parkInternal")
@ObsoleteWorkersApi
external internal fun parkInternal(id: Int, timeoutMicroseconds: Long, process: Boolean): Boolean

@GCUnsafeCall("Kotlin_Worker_getNameInternal")
@ObsoleteWorkersApi
external internal fun getWorkerNameInternal(id: Int): ExternalRCRef

@ExportForCppRuntime
@ObsoleteWorkersApi
internal fun ThrowWorkerAlreadyTerminated(): Unit =
        throw IllegalStateException("Worker is already terminated")

@ExportForCppRuntime
@ObsoleteWorkersApi
internal fun ThrowWrongWorkerOrAlreadyTerminated(): Unit =
        throw IllegalStateException("Worker is not current or already terminated")

@ExportForCppRuntime
@ObsoleteWorkersApi
internal fun ThrowFutureInvalidState(): Unit =
        throw IllegalStateException("Future is in an invalid state")

@ExportForCppRuntime
@ObsoleteWorkersApi
internal fun ThrowWorkerUnsupported(): Unit =
        throw UnsupportedOperationException("Workers are not supported")

@ExportForCppRuntime
@ObsoleteWorkersApi
internal fun WorkerLaunchpadForExecuteAfterJob(job: ExternalRCRef) {
    @Suppress("UNCHECKED_CAST")
    val operation = dereferenceExternalRCRef(job) as (() -> Unit)
    releaseExternalRCRef(job)
    disposeExternalRCRef(job)
    operation()
}

@ExportForCppRuntime
@ObsoleteWorkersApi
internal fun WorkerLaunchpadForRegularJob(jobArgument: NativePtr, job: CPointer<CFunction<(Any?) -> Any?>>): NativePtr {
    val argument = dereferenceExternalRCRef(jobArgument)
    releaseExternalRCRef(jobArgument)
    disposeExternalRCRef(jobArgument)
    val result = invokeFunctionPointer(job, argument)
    return createRetainedExternalRCRef(result)
}

@GCUnsafeCall("Kotlin_Worker_invokeFunctionPointer")
external private fun invokeFunctionPointer(job: CPointer<CFunction<(Any?) -> Any?>>, jobArgument: Any?): Any?

@PublishedApi
@ObsoleteWorkersApi
internal fun detachObjectGraphInternal(mode: Int, producer: () -> Any?): NativePtr {
    return createRetainedExternalRCRef(producer())
}

@PublishedApi
@ObsoleteWorkersApi
internal fun attachObjectGraphInternal(stable: NativePtr): Any? {
    val result = dereferenceExternalRCRef(stable)
    releaseExternalRCRef(stable)
    disposeExternalRCRef(stable)
    return result
}

@InternalForKotlinNative
@GCUnsafeCall("Kotlin_Worker_waitTermination")
@ObsoleteWorkersApi
external public fun waitWorkerTermination(worker: Worker)

@GCUnsafeCall("Kotlin_Worker_getPlatformThreadIdInternal")
@ObsoleteWorkersApi
external internal fun getPlatfromThreadIdInternal(id: Int): ULong

@GCUnsafeCall("Kotlin_Worker_getActiveWorkersInternal")
@ObsoleteWorkersApi
external internal fun getActiveWorkersInternal(): IntArray
