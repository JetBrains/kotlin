/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("RedundantSuspendModifier")

package kotlin.wasm.internal

import kotlin.coroutines.*
import kotlin.internal.DoNotInlineOnFirstStage
import kotlin.internal.UsedFromCompilerGeneratedCode
import kotlin.coroutines.intrinsics.*
import kotlin.wasm.internal.reftypes.contref1

@PublishedApi
@ExcludedFromCodegen
@UsedFromCompilerGeneratedCode
internal fun <T> getContinuation(): Continuation<T> =
    implementedAsIntrinsic

@PublishedApi
@Suppress("UNCHECKED_CAST")
@UsedFromCompilerGeneratedCode
internal suspend fun <T> returnIfSuspended(argument: Any?): T =
    argument as T

@PublishedApi
internal fun <T> interceptContinuationIfNeeded(
    context: CoroutineContext,
    continuation: Continuation<T>
): Continuation<T> = context[ContinuationInterceptor]?.interceptContinuation(continuation) ?: continuation

@PublishedApi
@DoNotInlineOnFirstStage
@UsedFromCompilerGeneratedCode
internal inline suspend fun getCoroutineContext(): CoroutineContext = getContinuation<Any?>().context

@PublishedApi
@DoNotInlineOnFirstStage
@UsedFromCompilerGeneratedCode
internal inline suspend fun <T> suspendCoroutineUninterceptedOrReturn(block: (Continuation<T>) -> Any?): T {
    return suspendCoroutineUninterceptedOrReturnImpl<T>(block)
}

internal class WasmContinuation<T, R>(
    internal var wasmContBox: contref1,
    completion: Continuation<R>,
    rethrowExceptions: Boolean = false
) : CoroutineImpl<T, R>(completion, rethrowExceptions) {
    var isRunning: Boolean = false

    val resultValue: Any? get() = result
    override fun doResume(): Any? {
        if (isResumed) {
            isResumed = false  // signal: sync resume happened, result stored in this.result
            return COROUTINE_SUSPENDED
        }
        isResumed = true
        isRunning = true
        val resumeResult: ResumeIntrinsicResult = exception?.let {
            resumeThrowImpl(it, wasmContBox)
        } ?: resumeWithImpl(this, wasmContBox)
        isResumed = false
        isRunning = false
        wasSuspended = true
        wasmContBox = resumeResult.remainingFunction ?: return resumeResult.result
        return COROUTINE_SUSPENDED
    }

    override fun resumeWith(result: Result<T>) {
        if (isRunning) {
            // Sync re-entrant resume: store result and signal, but don't call doResume()
            this.result = result.getOrNull()
            this.exception = result.exceptionOrNull()
            isResumed = false  // signal to suspendCoroutineUninterceptedOrReturnImpl
            return
        }
        super.resumeWith(result)
    }
}

internal fun resumeWithImpl(result: Any?, wasmContinuation: contref1): ResumeIntrinsicResult =
    resumeWithIntrinsic()

internal fun resumeThrowImpl(objectToThrow: Throwable, cont: contref1): ResumeIntrinsicResult =
    resumeThrowIntrinsic()

@ExcludedFromCodegen
internal fun resumeWithIntrinsic(): ResumeIntrinsicResult =
    implementedAsIntrinsic

@ExcludedFromCodegen
internal fun resumeThrowIntrinsic(): ResumeIntrinsicResult {
    implementedAsIntrinsic
}

internal class ResumeIntrinsicResult(
    val remainingFunction: contref1?,
    val result: Any?,
)

@Suppress("UNUSED")
@UsedFromCompilerGeneratedCode
internal fun buildResumeIntrinsicSuspendResult(
    suspendBody: Any?,
    remainingFunction: contref1,
): ResumeIntrinsicResult {
    return ResumeIntrinsicResult(remainingFunction, null)
}

@Suppress("UNUSED")
@UsedFromCompilerGeneratedCode
internal fun buildResumeIntrinsicValueResult(value: Any?): ResumeIntrinsicResult {
    return ResumeIntrinsicResult(nullableContrefIntrinsic(), value)
}

@ExcludedFromCodegen
internal fun nullableContrefIntrinsic(): contref1? {
    implementedAsIntrinsic
}

@PublishedApi
@Suppress("USELESS_CAST", "UNCHECKED_CAST", "RETURN_VALUE_NOT_USED")
internal suspend inline fun <T> suspendCoroutineUninterceptedOrReturnImpl(block: (Continuation<T>) -> Any?): T {
    val continuation = getContinuation<T>() as Continuation<T>
    val c = continuation as CoroutineImpl<*, *>
    c.wasSuspended = true
    c.isResumed = true
    val blockResult = block(continuation)
    if (blockResult !== COROUTINE_SUSPENDED) return (continuation as CoroutineImpl<*, *>).result as T
    if (!(continuation as CoroutineImpl<*, *>).isResumed) {
        val c = continuation as CoroutineImpl<*, *>
        val e = c.exception
        if (e != null) throw e
        // isResumed was reset to false by doResume() early-return path:
        // x.resume() was called synchronously inside block — result is already in continuation.result
        return (continuation as CoroutineImpl<*, *>).result as T
    }
    c.isResumed = false
    suspendIntrinsic(null)
    val e = c.exception
    if (e != null) throw e
    return continuation.result as T
}

@UsedFromCompilerGeneratedCode
@PublishedApi
@Suppress("UNUSED_PARAMETER")
@ExcludedFromCodegen
internal fun suspendIntrinsic(x: Any?): Any? {
    implementedAsIntrinsic
}

private fun <R> resumeWasmContinuationAndReturnResult(contref1: contref1, completion: Continuation<R>): Any? {
    val wasmContinuation = WasmContinuation<Continuation<R>, R>(contref1, completion, rethrowExceptions = true)
    wasmContinuation.resume(completion)
    return if (wasmContinuation.wasSuspended) COROUTINE_SUSPENDED else wasmContinuation.resultValue
}

internal fun <T> suspendFunction0ToContrefImpl(f: (suspend () -> T)): contref1 {
    return suspendFunction0ToContref(f)
}

internal fun <R, T> suspendFunction1ToContrefImpl(f: (suspend R.() -> T), receiver: R): contref1 {
    return suspendFunction1ToContref(f, receiver)
}

internal fun <R, P, T> suspendFunction2ToContrefImpl(f: (suspend R.(P) -> T), receiver: R, param: P): contref1 {
    return suspendFunction2ToContref(f, receiver, param)
}

@Suppress("UNUSED_PARAMETER")
@ExcludedFromCodegen
@PublishedApi
internal fun <T> suspendFunction0ToContref(f: (suspend () -> T)): contref1 {
    implementedAsIntrinsic
}

@Suppress("UNUSED_PARAMETER")
@ExcludedFromCodegen
@PublishedApi
internal fun <R, T> suspendFunction1ToContref(f: (suspend R.() -> T), receiver: R): contref1 {
    implementedAsIntrinsic
}

@Suppress("UNUSED_PARAMETER")
@ExcludedFromCodegen
@PublishedApi
internal fun <R, P, T> suspendFunction2ToContref(f: (suspend R.(P) -> T), receiver: R, param: P): contref1 {
    implementedAsIntrinsic
}

@PublishedApi
@Suppress("UNCHECKED_CAST")
internal fun <T> startCoroutineUninterceptedOrReturn0Impl(
    f: (suspend () -> T),
    completion: Continuation<T>,
): Any? {
    return resumeWasmContinuationAndReturnResult(suspendFunction0ToContrefImpl(f), completion)
}

@PublishedApi
@Suppress("UNCHECKED_CAST")
internal fun <R, T> startCoroutineUninterceptedOrReturn1Impl(
    f: (suspend R.() -> T),
    receiver: R,
    completion: Continuation<T>,
): Any? {
    return resumeWasmContinuationAndReturnResult(suspendFunction1ToContrefImpl(f, receiver), completion)
}

@PublishedApi
@Suppress("UNCHECKED_CAST")
internal fun <R, P, T> startCoroutineUninterceptedOrReturn2Impl(
    f: (suspend R.(P) -> T),
    receiver: R,
    param: P,
    completion: Continuation<T>
): Any? {
    return resumeWasmContinuationAndReturnResult(suspendFunction2ToContrefImpl(f, receiver, param), completion)}

@PublishedApi
@SinceKotlin("1.3")
@UsedFromCompilerGeneratedCode
internal val EmptyContinuation: Continuation<Any?> = Continuation(EmptyCoroutineContext) { result ->
    val _ = result.getOrThrow()
}
