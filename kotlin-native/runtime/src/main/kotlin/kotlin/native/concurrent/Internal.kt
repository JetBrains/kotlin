/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
@file:OptIn(ExperimentalForeignApi::class)
package kotlin.native.concurrent

import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.internal.DescribeObjectForDebugging
import kotlin.native.internal.ExportForCppRuntime
import kotlin.native.internal.GCUnsafeCall
import kotlin.native.internal.InternalForKotlinNative
import kotlin.native.internal.debugDescription
import kotlin.native.identityHashCode
import kotlin.reflect.KClass
import kotlinx.cinterop.*

@GCUnsafeCall("Kotlin_Any_isShareable")
@FreezingIsDeprecated
external internal fun Any?.isShareable(): Boolean

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

@kotlin.native.internal.ExportForCompiler
@ObsoleteWorkersApi
internal fun executeImpl(worker: Worker, mode: TransferMode, producer: () -> Any?,
                         job: CPointer<CFunction<*>>): Future<Any?> =
        Future<Any?>(executeInternal(worker.id, mode.value, producer, job))

@GCUnsafeCall("Kotlin_Worker_startInternal")
@ObsoleteWorkersApi
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
internal fun ThrowCannotTransferOwnership(): Unit =
        throw IllegalStateException("Unable to transfer object: it is still owned elsewhere")

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

@GCUnsafeCall("Kotlin_Worker_freezeInternal")
@FreezingIsDeprecated
internal external fun freezeInternal(it: Any?)

@GCUnsafeCall("Kotlin_Worker_isFrozenInternal")
@FreezingIsDeprecated
internal external fun isFrozenInternal(it: Any?): Boolean

@ExportForCppRuntime
@FreezingIsDeprecated
internal fun ThrowFreezingException(toFreeze: Any, blocker: Any): Nothing =
        throw FreezingException(toFreeze, blocker)

@ExportForCppRuntime
@FreezingIsDeprecated
@OptIn(ExperimentalNativeApi::class)
internal fun ThrowInvalidMutabilityException(where: Any): Nothing {
    val description = debugDescription(where::class, where.identityHashCode())
    throw InvalidMutabilityException("mutation attempt of frozen $description")
}

@ExportForCppRuntime
@FreezingIsDeprecated
internal fun ThrowIllegalObjectSharingException(typeInfo: NativePtr, address: NativePtr) {
    val description = DescribeObjectForDebugging(typeInfo, address)
    throw IncorrectDereferenceException("illegal attempt to access non-shared $description from other thread")
}

@GCUnsafeCall("Kotlin_AtomicReference_checkIfFrozen")
@FreezingIsDeprecated
external internal fun checkIfFrozen(ref: Any?)

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
