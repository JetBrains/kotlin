/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.native.concurrent

import kotlin.native.internal.ExportForCppRuntime

/**
 * Exception thrown whenever freezing is not possible.
 */
public class FreezingException(toFreeze: Any, blocker: Any) :
        RuntimeException("freezing of $toFreeze has failed, first blocker is $blocker")

/**
 * Exception thrown whenever we attempt to mutate frozen objects.
 */
public class InvalidMutabilityException(where: Any) :
        RuntimeException("mutation attempt of frozen $where (hash is 0x${where.hashCode().toString(16)})")

/**
 * Freezes object subgraph reachable from this object. Frozen objects can be freely
 * shared between threads/workers.
 */
fun <T> T.freeze(): T {
    freezeInternal(this)
    return this
}

val Any?.isFrozen
    get() = isFrozenInternal(this)


/**
 * This function ensures that if we see such an object during freezing attempt - freeze fails and FreezingException
 * is thrown. Is object is already frozen - FreezingException is thrown immediately.
 */
@SymbolName("Kotlin_Worker_ensureNeverFrozen")
external fun Any.ensureNeverFrozen()

@SymbolName("Kotlin_Worker_freezeInternal")
internal external fun freezeInternal(it: Any?)

@SymbolName("Kotlin_Worker_isFrozenInternal")
internal external fun isFrozenInternal(it: Any?): Boolean

@ExportForCppRuntime
internal fun ThrowFreezingException(toFreeze: Any, blocker: Any): Nothing =
        throw FreezingException(toFreeze, blocker)

@ExportForCppRuntime
internal fun ThrowInvalidMutabilityException(where: Any): Nothing = throw InvalidMutabilityException(where)
