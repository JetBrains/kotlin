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
internal fun <T> getContinuation(): Continuation<T> =
    implementedAsIntrinsic

@PublishedApi
@Suppress("UNCHECKED_CAST")
internal suspend fun <T> returnIfSuspended(argument: Any?): T =
    argument as T

@PublishedApi
internal fun <T> interceptContinuationIfNeeded(
    context: CoroutineContext,
    continuation: Continuation<T>
): Continuation<T> = context[ContinuationInterceptor]?.interceptContinuation(continuation) ?: continuation

@UsedFromCompilerGeneratedCode
@PublishedApi
@DoNotInlineOnFirstStage
internal inline suspend fun getCoroutineContext(): CoroutineContext = getContinuation<Any?>().context

@UsedFromCompilerGeneratedCode
@PublishedApi
@DoNotInlineOnFirstStage
internal inline suspend fun <T> suspendCoroutineUninterceptedOrReturn(noinline block: (Continuation<T>) -> Any?): T {
    return suspendCoroutineUninterceptedOrReturnImpl<T>(block)
}

internal class WasmContinuationBox(
    internal val cont: contref1,
)

@Suppress("UNCHECKED_CAST")
internal class WasmContinuation<in T, R>(
    internal var wasmContBox: WasmContinuationBox?,
    private var isResumed: Boolean,
    private val completion: Continuation<R>,
) : Continuation<T> {
    override val context: CoroutineContext = completion.context
    private var _result: Any? = null
    private var _exception: Throwable? = null

    val result get() = _result
    val exception get() = _exception

    override fun resumeWith(result: Result<T>) {
        if (isResumed) error("Continuation is already resumed")
        wasmContBox?.let { contBox ->
            isResumed = true
            val resumeResult: ResumeIntrinsicResult = try {
                result.exceptionOrNull()?.let {
                    resumeThrowImpl(it, contBox.cont)
                } ?: resumeWithImpl(contBox.cont, result.value)
            } catch (e: Throwable) {
                _exception = e
                completion.resumeWithException(e)
                return@let
            }
            if (resumeResult.wasmContWrapper == null) {
                _result = resumeResult.result
                completion.resume(resumeResult.result as R)
            } else {
                _result = COROUTINE_SUSPENDED
            }
        } ?: error("Continuation is not set")
    }
}

internal fun resumeWithImpl(wasmContinuation: contref1, result: Any?): ResumeIntrinsicResult =
    resumeWithIntrinsic(wasmContinuation, result)

internal fun resumeThrowImpl(objectToThrow: Throwable, cont: contref1): ResumeIntrinsicResult =
    resumeThrowIntrinsic(objectToThrow, cont)

@Suppress("UNUSED_PARAMETER")
@ExcludedFromCodegen
internal fun resumeWithIntrinsic(wasmContinuation: contref1, result: Any?): ResumeIntrinsicResult {
    implementedAsIntrinsic
}

@Suppress("UNUSED_PARAMETER")
@ExcludedFromCodegen
internal fun resumeThrowIntrinsic(objectToThrow: Throwable, cont: contref1): ResumeIntrinsicResult {
    implementedAsIntrinsic
}

internal class ResumeIntrinsicResult(val wasmContWrapper: WasmContinuation<*, *>?, val result: Any?)

internal fun buildResumeIntrinsicSuspendResult(wasmContWrapper: Any?, wasmContRef: contref1): ResumeIntrinsicResult {
    wasmContWrapper as WasmContinuation<*, *>
    wasmContWrapper.wasmContBox = WasmContinuationBox(wasmContRef)
    return ResumeIntrinsicResult(wasmContWrapper, null)
}

internal fun buildResumeIntrinsicValueResult(value: Any?): ResumeIntrinsicResult {
    return ResumeIntrinsicResult(null, value)
}

@Suppress("UNUSED")
internal fun setWasmContinuation(cont: WasmContinuation<*, *>, b: contref1): Any? {
    cont.wasmContBox = WasmContinuationBox(b)
    return cont
}

@Suppress("UNUSED")
internal fun resumeCompletionWithValue(completion: Continuation<Any?>, value: Any?) {
    completion.resume(value)
}

@Suppress("UNUSED")
internal fun resumeCompletionWithException(completion: Continuation<Throwable>, exception: Throwable) {
    completion.resumeWithException(exception)
}

@PublishedApi
@Suppress("UNCHECKED_CAST")
internal suspend fun <T> suspendCoroutineUninterceptedOrReturnImpl(block: (Continuation<T>) -> Any?): T {
    val completion = getContinuation<T>()
    val remainingFunction = WasmContinuation<T, T>(null, false, completion)
    val result = block(remainingFunction)
    return if (result == COROUTINE_SUSPENDED) {
        suspendIntrinsic(remainingFunction) as T
    } else result as T
}

@UsedFromCompilerGeneratedCode
@PublishedApi
@Suppress("UNUSED_PARAMETER")
@ExcludedFromCodegen
internal fun suspendIntrinsic(cont: Continuation<*>): Any? {
    implementedAsIntrinsic
}

@Suppress("UNUSED_PARAMETER")
@ExcludedFromCodegen
@PublishedApi
internal fun <T> startCoroutineUninterceptedOrReturnIntrinsic0(
    f: (suspend () -> T),
    completion: Continuation<T>
): Any? {
    implementedAsIntrinsic
}

private fun <T> resumeWasmContinuationAndReturnResult(contref1: contref1, completion: Continuation<T>): Any? {
    val wasmContinuation = WasmContinuation<Continuation<T>, T>(null, false, completion)
    wasmContinuation.wasmContBox = WasmContinuationBox(contref1)
    wasmContinuation.resume(completion)
    return wasmContinuation.result
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
internal fun <T> startCoroutineUninterceptedOrReturn0Impl(
    f: (suspend () -> T),
    completion: Continuation<T>
): Any? {
    return resumeWasmContinuationAndReturnResult(suspendFunction0ToContrefImpl(f), completion)
}

@PublishedApi
internal fun <R, T> startCoroutineUninterceptedOrReturn1Impl(
    f: (suspend R.() -> T),
    receiver: R,
    completion: Continuation<T>
): Any? {
    return resumeWasmContinuationAndReturnResult(suspendFunction1ToContrefImpl(f, receiver), completion)
}

@PublishedApi
internal fun <R, P, T> startCoroutineUninterceptedOrReturn2Impl(
    f: (suspend R.(P) -> T),
    receiver: R,
    param: P,
    completion: Continuation<T>
): Any? {
    return resumeWasmContinuationAndReturnResult(suspendFunction2ToContrefImpl(f, receiver, param), completion)
}

@PublishedApi
@SinceKotlin("1.3")
internal val EmptyContinuation: Continuation<Any?> = Continuation(EmptyCoroutineContext) { result ->
    val _ = result.getOrThrow()
}
