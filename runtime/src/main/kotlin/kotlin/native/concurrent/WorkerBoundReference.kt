/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.native.concurrent

import kotlin.native.internal.*

@SymbolName("Kotlin_WorkerBoundReference_create")
external private fun createWorkerBoundReference(value: Any): NativePtr

@SymbolName("Kotlin_WorkerBoundReference_deref")
external private fun derefWorkerBoundReference(ref: NativePtr): Any

/**
 * A frozen shared reference to a Kotlin object
 *
 * Can be safely passed between workers, but [value] can only be accessed on the worker [WorkerBoundReference] was created on,
 * unless the referred object is frozen too
 *
 * Note: Garbage collector currently cannot free any reference cycles with [WorkerBoundReference] or [DisposableWorkerBoundReference] in them.
 * Consider using [DisposableWorkerBoundReference] to manually resolve cycles with explicit calls to [DisposableWorkerBoundReference.dispose]
 */
@Frozen
@NoReorderFields
@ExportTypeInfo("theWorkerBoundReferenceTypeInfo")
public class WorkerBoundReference<out T : Any>(value: T) {

    private val ptr = createWorkerBoundReference(value)

    /**
     * The referenced value.
     * @throws IncorrectDereferenceException if referred object is not frozen and current worker is different from the one created [this]
     */
    val value: T
        get() = @Suppress("UNCHECKED_CAST") (derefWorkerBoundReference(ptr) as T)
}

/**
 * A frozen shared reference to a Kotlin object
 *
 * Can be safely passed between workers, but [value] can only be accessed on the worker [DisposableWorkerBoundReference] was created on,
 * unless the referred object is frozen too
 * Garbage collector currently cannot free any reference cycles with [WorkerBoundReference] or [DisposableWorkerBoundReference] in them.
 * Call [dispose] manually to resolve cycles
 *
 * Note: This class has more expensive [value] getter than [WorkerBoundReference]. If you don't have reference
 * cycles with [WorkerBoundReference] or [DisposableWorkerBoundReference], consider using [WorkerBoundReference]
 */
@Frozen
public class DisposableWorkerBoundReference<out T : Any>(value: T) {

    private val ref: AtomicReference<WorkerBoundReference<T>?> = AtomicReference(WorkerBoundReference(value))

    /**
     * Free the reference. Any call to [DisposableWorkerBoundReference.value] after that will
     * fail with [IllegalStateException]
     */
    fun dispose() {
        ref.value = null
    }

    /**
     * The referenced value.
     * @throws IncorrectDereferenceException if referred object is not frozen and current worker is different from the one created [this]
     * @throws IllegalStateException if [DisposableWorkerBoundReference.dispose] was called on this reference.
     */
    val value: T
        get() = ref.value?.value ?: throw IllegalStateException("illegal attempt to dereference disposed $this")
}
