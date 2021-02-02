/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package kotlin.native

import kotlin.native.concurrent.isFrozen
import kotlin.native.concurrent.InvalidMutabilityException
import kotlin.native.internal.Escapes

/**
 * Initializes Kotlin runtime for the current thread, if not inited already.
 */
@SymbolName("Kotlin_initRuntimeIfNeeded")
external public fun initRuntimeIfNeeded(): Unit

/**
 * Deinitializes Kotlin runtime for the current thread, if was inited.
 * Cannot be called from Kotlin frames holding references, thus deprecated.
 */
@SymbolName("Kotlin_deinitRuntimeIfNeeded")
@Deprecated("Deinit runtime can not be called from Kotlin", level = DeprecationLevel.ERROR)
external public fun deinitRuntimeIfNeeded(): Unit

/**
 * Exception thrown when top level variable is accessed from incorrect execution context.
 */
public class IncorrectDereferenceException : RuntimeException {
    constructor() : super()

    constructor(message: String) : super(message)
}

/**
 * Typealias describing custom exception reporting hook.
 */
public typealias ReportUnhandledExceptionHook = Function1<Throwable, Unit>

/**
 * Install custom unhandled exception hook. Returns old hook, or null if it was not specified.
 * Hook is invoked whenever there's uncaught exception reaching boundaries of the Kotlin world,
 * i.e. top level main(), or when Objective-C to Kotlin call not marked with @Throws throws an exception.
 * Hook must be a frozen lambda, so that it could be called from any thread/worker.
 * Hook is invoked once, and is cleared afterwards, so that memory leak detection works as expected even
 * with custom exception hooks.
 */
public fun setUnhandledExceptionHook(hook: ReportUnhandledExceptionHook): ReportUnhandledExceptionHook? {
    if (Platform.memoryModel != MemoryModel.EXPERIMENTAL && !hook.isFrozen) {
        throw InvalidMutabilityException("Unhandled exception hook must be frozen")
    }
    return setUnhandledExceptionHook0(hook)
}

@SymbolName("Kotlin_setUnhandledExceptionHook")
@Escapes(0b01) // <hook> escapes
external private fun setUnhandledExceptionHook0(hook: ReportUnhandledExceptionHook): ReportUnhandledExceptionHook?

/**
 * Compute stable wrt potential object relocations by the memory manager identity hash code.
 * @return 0 for `null` object, identity hash code otherwise.
 */
@SymbolName("Kotlin_Any_hashCode")
public external fun Any?.identityHashCode(): Int