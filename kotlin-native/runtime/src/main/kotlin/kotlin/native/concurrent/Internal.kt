/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.native.concurrent

import kotlin.native.internal.DescribeObjectForDebugging
import kotlin.native.internal.ExportForCppRuntime
import kotlin.native.internal.GCCritical
import kotlin.native.internal.InternalForKotlinNative
import kotlin.native.internal.debugDescription
import kotlin.native.identityHashCode
import kotlin.reflect.KClass
import kotlinx.cinterop.*

// Implementation details.

@SymbolName("Kotlin_Worker_stateOfFuture")
external internal fun stateOfFuture(id: Int): Int

@SymbolName("Kotlin_Worker_consumeFuture")
@PublishedApi
external internal fun consumeFuture(id: Int): Any?

@SymbolName("Kotlin_Worker_waitForAnyFuture")
external internal fun waitForAnyFuture(versionToken: Int, millis: Int): Boolean

@SymbolName("Kotlin_Worker_versionToken")
external internal fun versionToken(): Int

@kotlin.native.internal.ExportForCompiler
internal fun executeImpl(worker: Worker, mode: TransferMode, producer: () -> Any?,
                         job: CPointer<CFunction<*>>): Future<Any?> =
        Future<Any?>(executeInternal(worker.id, mode.value, producer, job))

@SymbolName("Kotlin_Worker_startInternal")
external internal fun startInternal(errorReporting: Boolean, name: String?): Int

@SymbolName("Kotlin_Worker_currentInternal")
external internal fun currentInternal(): Int

@SymbolName("Kotlin_Worker_requestTerminationWorkerInternal")
external internal fun requestTerminationInternal(id: Int, processScheduledJobs: Boolean): Int

@SymbolName("Kotlin_Worker_executeInternal")
external internal fun executeInternal(
        id: Int, mode: Int, producer: () -> Any?, job: CPointer<CFunction<*>>): Int

@SymbolName("Kotlin_Worker_executeAfterInternal")
external internal fun executeAfterInternal(id: Int, operation: () -> Unit, afterMicroseconds: Long): Unit

@SymbolName("Kotlin_Worker_processQueueInternal")
external internal fun processQueueInternal(id: Int): Boolean

@SymbolName("Kotlin_Worker_parkInternal")
external internal fun parkInternal(id: Int, timeoutMicroseconds: Long, process: Boolean): Boolean

@SymbolName("Kotlin_Worker_getNameInternal")
external internal fun getWorkerNameInternal(id: Int): String?

@ExportForCppRuntime
internal fun ThrowWorkerUnsupported(): Unit =
        throw UnsupportedOperationException("Workers are not supported")

@ExportForCppRuntime
internal fun ThrowWorkerInvalidState(): Unit =
        throw IllegalStateException("Illegal transfer state")

@ExportForCppRuntime
internal fun WorkerLaunchpad(function: () -> Any?) = function()

@PublishedApi
@SymbolName("Kotlin_Worker_detachObjectGraphInternal")
@GCCritical // Modifies the root set and runs a Kotlin callback.
external internal fun detachObjectGraphInternal(mode: Int, producer: () -> Any?): NativePtr

@PublishedApi
@SymbolName("Kotlin_Worker_attachObjectGraphInternal")
@GCCritical // Modifies the root set via stable pointer adoption.
external internal fun attachObjectGraphInternal(stable: NativePtr): Any?

@SymbolName("Kotlin_Worker_freezeInternal")
// Modifies the object graph.
// TODO: Reconsider this annotation when freezing is implemented for the new MM.
@GCCritical
internal external fun freezeInternal(it: Any?)

@SymbolName("Kotlin_Worker_isFrozenInternal")
@GCCritical // Fast, just a flags check.
internal external fun isFrozenInternal(it: Any?): Boolean

@ExportForCppRuntime
internal fun ThrowFreezingException(toFreeze: Any, blocker: Any): Nothing =
        throw FreezingException(toFreeze, blocker)

@ExportForCppRuntime
internal fun ThrowInvalidMutabilityException(where: Any): Nothing {
    val description = debugDescription(where::class, where.identityHashCode())
    throw InvalidMutabilityException("mutation attempt of frozen $description")
}

@ExportForCppRuntime
internal fun ThrowIllegalObjectSharingException(typeInfo: NativePtr, address: NativePtr) {
    val description = DescribeObjectForDebugging(typeInfo, address)
    throw IncorrectDereferenceException("illegal attempt to access non-shared $description from other thread")
}

@SymbolName("Kotlin_AtomicReference_checkIfFrozen")
@GCCritical // Fast, just a flags check.
external internal fun checkIfFrozen(ref: Any?)

@InternalForKotlinNative
@SymbolName("Kotlin_Worker_waitTermination")
external public fun waitWorkerTermination(worker: Worker)
