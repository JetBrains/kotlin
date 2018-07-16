/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.coroutines.experimental.migration

import kotlin.coroutines.experimental.Continuation as ExperimentalContinuation
import kotlin.coroutines.experimental.CoroutineContext as ExperimentalCoroutineContext
import kotlin.coroutines.experimental.AbstractCoroutineContextElement as ExperimentalAbstractCoroutineContextElement
import kotlin.coroutines.experimental.EmptyCoroutineContext as ExperimentalEmptyCoroutineContext
import kotlin.coroutines.experimental.ContinuationInterceptor as ExperimentalContinuationInterceptor
import kotlin.coroutines.experimental.intrinsics.COROUTINE_SUSPENDED as EXPERIMENTAL_COROUTINE_SUSPENDED
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*


/**
 * Adapter to invoke experimental suspending function.
 *
 * To invoke experimental suspending function `foo(args)` that returns value of some type `Result`
 * from Kotlin 1.3 use the following code:
 *
 * ```
 * invokeExperimentalSuspend<Result> { foo(args, it) }
 * ```
 */
@SinceKotlin("1.3")
public suspend inline fun <T> invokeExperimentalSuspend(crossinline invocation: (ExperimentalContinuation<T>) -> Any?): T =
    suspendCoroutineUninterceptedOrReturn { continuation ->
        val result = invocation(continuation.toExperimentalContinuation())
        @Suppress("UNCHECKED_CAST")
        if (result === EXPERIMENTAL_COROUTINE_SUSPENDED) COROUTINE_SUSPENDED else result as T
    }

/**
 * Coverts reference to suspending function to experimental suspending function.
 */
@SinceKotlin("1.3")
public fun <T> (suspend () -> T).toExperimentalSuspendFunction(): (ExperimentalContinuation<T>) -> Any? =
    ExperimentalSuspendFunction0Migration(this)

/**
 * Coverts reference to experimental suspending function to suspending function.
 */
@SinceKotlin("1.3")
public fun <T> ((ExperimentalContinuation<T>) -> Any?).toSuspendFunction(): suspend () -> T =
    (this as? ExperimentalSuspendFunction0Migration)?.function ?: SuspendFunction0Migration(this)::invoke

/**
 * Coverts reference to suspending function with receiver to experimental suspending function.
 */
@SinceKotlin("1.3")
public fun <R, T> (suspend (R) -> T).toExperimentalSuspendFunction(): (R, ExperimentalContinuation<T>) -> Any? =
    ExperimentalSuspendFunction1Migration(this)

/**
 * Coverts reference to experimental suspending function with receiver to suspending function.
 */
@SinceKotlin("1.3")
public fun <R, T> ((R, ExperimentalContinuation<T>) -> Any?).toSuspendFunction(): suspend (R) -> T =
    (this as? ExperimentalSuspendFunction1Migration)?.function ?: SuspendFunction1Migration(this)::invoke

/**
 * Converts [Continuation] to [ExperimentalContinuation].
 */
@SinceKotlin("1.3")
public fun <T> Continuation<T>.toExperimentalContinuation(): ExperimentalContinuation<T> =
    (this as? ContinuationMigration<T>)?.continuation ?: ExperimentalContinuationMigration(this)

/**
 * Converts [ExperimentalContinuation] to [Continuation].
 */
@SinceKotlin("1.3")
public fun <T> ExperimentalContinuation<T>.toContinuation(): Continuation<T> =
    (this as? ExperimentalContinuationMigration<T>)?.continuation ?: ContinuationMigration(this)

/**
 * Converts [CoroutineContext] to [ExperimentalCoroutineContext].
 */
@SinceKotlin("1.3")
public fun CoroutineContext.toExperimentalCoroutineContext(): ExperimentalCoroutineContext {
    val interceptor = get(ContinuationInterceptor.Key)
    val migration = get(ContextMigration.Key)
    val remainder = minusKey(ContinuationInterceptor.Key).minusKey(ContextMigration.Key)
    val original = migration?.context ?: ExperimentalEmptyCoroutineContext
    val result = if (remainder === EmptyCoroutineContext) original else original + ExperimentalContextMigration(remainder)
    return if (interceptor == null) result else result + interceptor.toExperimentalContinuationInterceptor()
}

/**
 * Converts [ExperimentalCoroutineContext] to [CoroutineContext].
 */
@SinceKotlin("1.3")
public fun ExperimentalCoroutineContext.toCoroutineContext(): CoroutineContext {
    val interceptor = get(ExperimentalContinuationInterceptor.Key)
    val migration = get(ExperimentalContextMigration.Key)
    val remainder = minusKey(ExperimentalContinuationInterceptor.Key).minusKey(ExperimentalContextMigration.Key)
    val original = migration?.context ?: EmptyCoroutineContext
    val result = if (remainder === ExperimentalEmptyCoroutineContext) original else original + ContextMigration(remainder)
    return if (interceptor == null) result else result + interceptor.toContinuationInterceptor()
}

/**
 * Converts [ContinuationInterceptor] to [ExperimentalContinuationInterceptor].
 */
@SinceKotlin("1.3")
public fun ContinuationInterceptor.toExperimentalContinuationInterceptor(): ExperimentalContinuationInterceptor =
    (this as? ContinuationInterceptorMigration)?.interceptor ?: ExperimentalContinuationInterceptorMigration(this)

/**
 * Converts [ExperimentalContinuationInterceptor] to [ContinuationInterceptor].
 */
@SinceKotlin("1.3")
public fun ExperimentalContinuationInterceptor.toContinuationInterceptor(): ContinuationInterceptor =
    (this as? ExperimentalContinuationInterceptorMigration)?.interceptor ?: ContinuationInterceptorMigration(this)

// ------------------ converter classes ------------------
// Their name starts with "Experimental" if they implement the corresponding Experimental interfaces

private class ExperimentalSuspendFunction0Migration<T>(val function: suspend () -> T) : (ExperimentalContinuation<T>) -> Any? {
    override fun invoke(continuation: ExperimentalContinuation<T>): Any? {
        val result = function.startCoroutineUninterceptedOrReturn(continuation.toContinuation())
        return if (result === COROUTINE_SUSPENDED) EXPERIMENTAL_COROUTINE_SUSPENDED else result
    }
}

private class SuspendFunction0Migration<T>(val function: (ExperimentalContinuation<T>) -> Any?) {
    suspend fun invoke(): T = suspendCoroutineUninterceptedOrReturn { continuation ->
        val result = function(continuation.toExperimentalContinuation())
        if (result === EXPERIMENTAL_COROUTINE_SUSPENDED) COROUTINE_SUSPENDED else result
    }
}

private class ExperimentalSuspendFunction1Migration<R, T>(val function: suspend (R) -> T) : (R, ExperimentalContinuation<T>) -> Any? {
    override fun invoke(receiver: R, continuation: ExperimentalContinuation<T>): Any? {
        val result = function.startCoroutineUninterceptedOrReturn(receiver, continuation.toContinuation())
        return if (result === COROUTINE_SUSPENDED) EXPERIMENTAL_COROUTINE_SUSPENDED else result
    }
}

private class SuspendFunction1Migration<R, T>(val function: (R, ExperimentalContinuation<T>) -> Any?) {
    suspend fun invoke(receiver: R): T = suspendCoroutineUninterceptedOrReturn { continuation ->
        val result = function(receiver, continuation.toExperimentalContinuation())
        if (result === EXPERIMENTAL_COROUTINE_SUSPENDED) COROUTINE_SUSPENDED else result
    }
}

private class ExperimentalContinuationMigration<T>(val continuation: Continuation<T>): ExperimentalContinuation<T> {
    override val context = continuation.context.toExperimentalCoroutineContext()
    override fun resume(value: T) = continuation.resume(value)
    override fun resumeWithException(exception: Throwable) = continuation.resumeWithException(exception)
}

private class ContinuationMigration<T>(val continuation: ExperimentalContinuation<T>): Continuation<T> {
    override val context: CoroutineContext = continuation.context.toCoroutineContext()
    override fun resumeWith(result: SuccessOrFailure<T>) {
        result
            .onSuccess { continuation.resume(it) }
            .onFailure { continuation.resumeWithException(it) }
    }
}

private class ExperimentalContextMigration(val context: CoroutineContext): ExperimentalAbstractCoroutineContextElement(Key) {
    companion object Key : ExperimentalCoroutineContext.Key<ExperimentalContextMigration>
}

private class ContextMigration(val context: ExperimentalCoroutineContext): AbstractCoroutineContextElement(Key) {
    companion object Key : CoroutineContext.Key<ContextMigration>
}

private class ExperimentalContinuationInterceptorMigration(val interceptor: ContinuationInterceptor) : ExperimentalContinuationInterceptor {
    override val key: ExperimentalCoroutineContext.Key<*>
        get() = ExperimentalContinuationInterceptor.Key

    override fun <T> interceptContinuation(continuation: ExperimentalContinuation<T>): ExperimentalContinuation<T> =
        interceptor.interceptContinuation(continuation.toContinuation()).toExperimentalContinuation()
}

private class ContinuationInterceptorMigration(val interceptor: ExperimentalContinuationInterceptor) : ContinuationInterceptor {
    override val key: CoroutineContext.Key<*>
        get() = ContinuationInterceptor.Key

    override fun <T> interceptContinuation(continuation: Continuation<T>): Continuation<T> =
        interceptor.interceptContinuation(continuation.toExperimentalContinuation()).toContinuation()
}

