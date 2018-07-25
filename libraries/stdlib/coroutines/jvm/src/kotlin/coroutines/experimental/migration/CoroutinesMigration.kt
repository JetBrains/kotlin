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

