/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

@file:OptIn(ExperimentalStdlibApi::class)

package kotlin.native

import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.internal.*
import kotlin.native.internal.escapeAnalysis.Escapes

/**
 * Initializes Kotlin runtime for the current thread, if not inited already.
 */
@Deprecated("Initializing runtime is not possible in the new memory model.")
@DeprecatedSinceKotlin(warningSince = "1.9", errorSince = "2.1")
public fun initRuntimeIfNeeded() {}


/**
 * Exception thrown when top level variable is accessed from incorrect execution context.
 */
@Deprecated("Support for the legacy memory manager has been completely removed. Usages of this exception can be safely dropped.")
@DeprecatedSinceKotlin(errorSince = "2.1")
public class IncorrectDereferenceException : RuntimeException {
    public constructor() : super()

    public constructor(message: String) : super(message)
}


/**
 * Typealias describing custom exception reporting hook.
 */
@ExperimentalNativeApi
public typealias ReportUnhandledExceptionHook = Function1<Throwable, Unit>

/**
 * Installs an unhandled exception hook and returns an old hook, or `null` if no user-defined hooks were previously set.
 *
 * The hook is invoked whenever there is an uncaught exception reaching the boundaries of the Kotlin world,
 * i.e. top-level `main()`, worker boundary, or when Objective-C to Kotlin call not marked with `@Throws` throws an exception.
 *
 * The hook is in full control of how to process an unhandled exception and proceed further.
 * For hooks that terminate an application, it is recommended to use [terminateWithUnhandledException] to
 * be consistent with a default behaviour when no hooks are set.
 *
 * Set or default hook is also invoked by [processUnhandledException].
 */
@ExperimentalNativeApi
public fun setUnhandledExceptionHook(hook: ReportUnhandledExceptionHook?): ReportUnhandledExceptionHook? {
    return UnhandledExceptionHookHolder.hook.exchange(hook)
}

/**
 * Returns a user-defined unhandled exception hook set by [setUnhandledExceptionHook] or `null` if no user-defined hooks were set.
 */
@ExperimentalNativeApi
@SinceKotlin("1.6")
public fun getUnhandledExceptionHook(): ReportUnhandledExceptionHook? {
    return UnhandledExceptionHookHolder.hook.load()
}

/**
 * Performs the default processing of unhandled exception.
 *
 * If user-defined hook set by [setUnhandledExceptionHook] is present, calls it and returns.
 * If the hook is not present, calls [terminateWithUnhandledException] with [throwable].
 * If the hook fails with exception, calls [terminateWithUnhandledException] with exception from the hook.
 */
@ExperimentalNativeApi
@SinceKotlin("1.6")
@GCUnsafeCall("Kotlin_processUnhandledException")
@Escapes(0b01) // throwable may be passed to the user code via unhandled exception hook.
public external fun processUnhandledException(throwable: Throwable): Unit

/*
 * Terminates the program with the given [throwable] as an unhandled exception.
 * User-defined hooks installed with [setUnhandledExceptionHook] are not invoked.
 *
 * `terminateWithUnhandledException` can be used to emulate an abrupt termination of the application with an uncaught exception.
 */
@ExperimentalNativeApi
@SinceKotlin("1.6")
@GCUnsafeCall("Kotlin_terminateWithUnhandledException")
@Escapes.Nothing // this function never returns.
public external fun terminateWithUnhandledException(throwable: Throwable): Nothing

/**
 * Compute stable wrt potential object relocations by the memory manager identity hash code.
 * @return 0 for `null` object, identity hash code otherwise.
 */
@ExperimentalNativeApi
@GCUnsafeCall("Kotlin_Any_hashCode")
@Escapes.Nothing
public external fun Any?.identityHashCode(): Int
