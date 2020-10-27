/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.native.concurrent

import kotlin.native.internal.*

@SymbolName("Kotlin_WorkerBoundReference_create")
external private fun createWorkerBoundReference(value: Any): NativePtr

@SymbolName("Kotlin_WorkerBoundReference_deref")
external private fun derefWorkerBoundReference(ref: NativePtr): Any?

@SymbolName("Kotlin_WorkerBoundReference_describe")
external private fun describeWorkerBoundReference(ref: NativePtr): String

/**
 * A shared reference to a Kotlin object that doesn't freeze the referred object when it gets frozen itself.
 *
 * After freezing can be safely passed between workers, but [value] can only be accessed on
 * the worker [WorkerBoundReference] was created on, unless the referred object is frozen too.
 *
 * Note: Garbage collector currently cannot free any reference cycles with frozen [WorkerBoundReference] in them.
 * To resolve such cycles consider using [AtomicReference<WorkerBoundReference?>] which can be explicitly
 * nulled out.
 */
@NoReorderFields
@ExportTypeInfo("theWorkerBoundReferenceTypeInfo")
@HasFinalizer
public class WorkerBoundReference<out T : Any>(value: T) {

    private var ptr = NativePtr.NULL
    private val ownerName = Worker.current.name
    private var valueBeforeFreezing: T? = value

    private val valueDescription
        get() = describeWorkerBoundReference(ptr)

    /**
     * The referenced value.
     * @throws IncorrectDereferenceException if referred object is not frozen and current worker is different from the one created [this].
     */
    val value: T
        get() = valueOrNull ?: throw IncorrectDereferenceException("illegal attempt to access non-shared $valueDescription bound to `$ownerName` from `${Worker.current.name}`")

    /**
     * The referenced value or null if referred object is not frozen and current worker is different from the one created [this].
     */
    val valueOrNull: T?
        get() = valueBeforeFreezing ?: @Suppress("UNCHECKED_CAST") (derefWorkerBoundReference(ptr) as T?)

    /**
     * Worker that [value] is bound to.
     */
    val worker: Worker = Worker.current

    @ExportForCppRuntime("Kotlin_WorkerBoundReference_freezeHook")
    private fun freezeHook() {
        // If this hook was already run, do nothing.
        if (valueBeforeFreezing == null)
            return
        ptr = createWorkerBoundReference(valueBeforeFreezing!!)
        valueBeforeFreezing = null
    }
}
