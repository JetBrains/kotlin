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

// Implementation details.

@GCUnsafeCall("Kotlin_Worker_stateOfFuture")
@ObsoleteWorkersApi
external internal fun stateOfFuture(id: Int): Int

@GCUnsafeCall("Kotlin_Worker_consumeFuture")
@PublishedApi
@ObsoleteWorkersApi
external internal fun consumeFuture(id: Int): Any?

@GCUnsafeCall("Kotlin_Worker_waitForAnyFuture")
@ObsoleteWorkersApi
external internal fun waitForAnyFuture(versionToken: Int, millis: Int): Boolean

@GCUnsafeCall("Kotlin_Worker_versionToken")
@ObsoleteWorkersApi
external internal fun versionToken(): Int

@ExportForCompiler
@ObsoleteWorkersApi
internal fun executeImpl(worker: Worker, mode: TransferMode, producer: () -> Any?,
                         job: CPointer<CFunction<*>>): Future<Any?> =
        Future<Any?>(executeInternal(worker.id, mode.value, producer, job))

@GCUnsafeCall("Kotlin_Worker_startInternal")
@ObsoleteWorkersApi
@Escapes(0b10) // name is stored in the Worker instance.
external internal fun startInternal(errorReporting: Boolean, name: String?): Int

@GCUnsafeCall("Kotlin_Worker_currentInternal")
@ObsoleteWorkersApi
external internal fun currentInternal(): Int

@GCUnsafeCall("Kotlin_Worker_requestTerminationWorkerInternal")
@ObsoleteWorkersApi
external internal fun requestTerminationInternal(id: Int, processScheduledJobs: Boolean): Int

@GCUnsafeCall("Kotlin_Worker_executeInternal")
@ObsoleteWorkersApi
external internal fun executeInternal(
        id: Int, mode: Int, producer: () -> Any?, job: CPointer<CFunction<*>>): Int

@GCUnsafeCall("Kotlin_Worker_executeAfterInternal")
@ObsoleteWorkersApi
@Escapes(0b10) // operation escapes into stable ref.
external internal fun executeAfterInternal(id: Int, operation: () -> Unit, afterMicroseconds: Long): Unit

@GCUnsafeCall("Kotlin_Worker_processQueueInternal")
@ObsoleteWorkersApi
external internal fun processQueueInternal(id: Int): Boolean

@GCUnsafeCall("Kotlin_Worker_parkInternal")
@ObsoleteWorkersApi
external internal fun parkInternal(id: Int, timeoutMicroseconds: Long, process: Boolean): Boolean

@GCUnsafeCall("Kotlin_Worker_getNameInternal")
@ObsoleteWorkersApi
external internal fun getWorkerNameInternal(id: Int): String?

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
internal fun WorkerLaunchpad(function: () -> Any?) = function()

@PublishedApi
@GCUnsafeCall("Kotlin_Worker_detachObjectGraphInternal")
@ObsoleteWorkersApi
external internal fun detachObjectGraphInternal(mode: Int, producer: () -> Any?): NativePtr

@PublishedApi
@GCUnsafeCall("Kotlin_Worker_attachObjectGraphInternal")
@ObsoleteWorkersApi
external internal fun attachObjectGraphInternal(stable: NativePtr): Any?

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
