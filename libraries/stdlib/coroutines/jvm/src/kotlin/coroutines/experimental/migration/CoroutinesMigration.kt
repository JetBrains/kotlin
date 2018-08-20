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
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*


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

internal fun <R> ((Continuation<R>) -> Any?).toExperimentalSuspendFunction(): (ExperimentalContinuation<R>) -> Any? =
    ExperimentalSuspendFunction0Migration(this)

internal fun <T1, R> ((T1, Continuation<R>) -> Any?).toExperimentalSuspendFunction(): (T1, ExperimentalContinuation<R>) -> Any? =
    ExperimentalSuspendFunction1Migration(this)

internal fun <T1, T2, R> ((T1, T2, Continuation<R>) -> Any?).toExperimentalSuspendFunction(): (T1, T2, ExperimentalContinuation<R>) -> Any? =
    ExperimentalSuspendFunction2Migration(this)

private class ExperimentalSuspendFunction0Migration<R>(
    val function: (Continuation<R>) -> Any?
) : (ExperimentalContinuation<R>) -> Any? {
    override fun invoke(continuation: ExperimentalContinuation<R>): Any? {
        return function(continuation.toContinuation())
    }
}

private class ExperimentalSuspendFunction1Migration<T1, R>(
    val function: (T1, Continuation<R>) -> Any?
) : (T1, ExperimentalContinuation<R>) -> Any? {
    override fun invoke(t1: T1, continuation: ExperimentalContinuation<R>): Any? {
        return function(t1, continuation.toContinuation())
    }
}

private class ExperimentalSuspendFunction2Migration<T1, T2, R>(
    val function: (T1, T2, Continuation<R>) -> Any?
) : (T1, T2, ExperimentalContinuation<R>) -> Any? {
    override fun invoke(t1: T1, t2: T2, continuation: ExperimentalContinuation<R>): Any? {
        return function(t1, t2, continuation.toContinuation())
    }
}
