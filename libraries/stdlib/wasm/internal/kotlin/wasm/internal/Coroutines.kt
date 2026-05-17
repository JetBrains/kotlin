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
import kotlin.wasm.internal.reftypes.contref0
import kotlin.wasm.internal.suspendFunction0ToContref
import kotlin.wasm.internal.suspendFunction2ToContref

@PublishedApi
internal class SuspensionMarker(var wasSuspended: Boolean = false) : AbstractCoroutineContextElement(Key) {
    companion object Key : CoroutineContext.Key<SuspensionMarker>
}

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
internal inline suspend fun getCoroutineContext(): CoroutineContext = getContinuation<Any?>().context.minusKey(SuspensionMarker)

@UsedFromCompilerGeneratedCode
@PublishedApi
@DoNotInlineOnFirstStage
internal inline suspend fun <T> suspendCoroutineUninterceptedOrReturn(noinline block: (Continuation<T>) -> Any?): T {
    return suspendCoroutineUninterceptedOrReturnImpl<T>(block)
}

internal class WasmContinuationBox(internal var cont: contref0?)

internal class WasmContinuation<T, R>(
    internal var wasmContBox: WasmContinuationBox,
    completion: Continuation<R>,
    rethrowExceptions: Boolean = false,
    var synthetic: Boolean = false,
    var syntheticResumeValue: Result<T>? = null
) : CoroutineImpl<T, R>(completion, rethrowExceptions) {
    internal var isResumed = false
    internal var isFreshInstance = true
    override fun doResume(): Any? {
        require(!isResumed) { "WasmContinuation can be resumed only once" }
        isResumed = true
        val resumeResult: ResumeIntrinsicResult = exception?.let {
            resumeThrowImpl(it, wasmContBox.cont!!)
        } ?: resumeWithImpl(wasmContBox.cont!!)
        if (resumeResult.cont != null) {
            resumeResult.cont.synthetic = false
            wasmContBox = resumeResult.cont.wasmContBox
            isResumed = false
            return COROUTINE_SUSPENDED
        } else {
            return resumeResult.result
        }
    }

    override fun resumeWith(result: Result<T>) {
        if (synthetic) {
            syntheticResumeValue = result
            synthetic = false
            return
        } else {
            require(syntheticResumeValue == null)
            super.resumeWith(result)
        }
    }
}

internal fun resumeWithImpl(wasmContinuation: contref0): ResumeIntrinsicResult =
    resumeWithIntrinsic(wasmContinuation)

internal fun resumeThrowImpl(objectToThrow: Throwable, cont: contref0): ResumeIntrinsicResult =
    resumeThrowIntrinsic(objectToThrow, cont)

@Suppress("UNUSED_PARAMETER")
@ExcludedFromCodegen
internal fun resumeWithIntrinsic(wasmContinuation: contref0): ResumeIntrinsicResult {
    implementedAsIntrinsic
}

@Suppress("UNUSED_PARAMETER")
@ExcludedFromCodegen
internal fun resumeThrowIntrinsic(objectToThrow: Throwable, cont: contref0): ResumeIntrinsicResult {
    implementedAsIntrinsic
}

internal class ResumeIntrinsicResult(
    val cont: WasmContinuation<*, *>?,
    val result: Any?,
)

@Suppress("UNUSED")
@UsedFromCompilerGeneratedCode
internal fun buildResumeIntrinsicSuspendResult(
    cont: Any?,
    remainingFunction: contref0,
): ResumeIntrinsicResult {
    return ResumeIntrinsicResult((cont as WasmContinuation<*, *>?)?.also { it.wasmContBox.cont = remainingFunction }, null)
}

@Suppress("UNUSED")
@UsedFromCompilerGeneratedCode
internal fun buildResumeIntrinsicValueResult(value: Any?): ResumeIntrinsicResult {
    return ResumeIntrinsicResult(null, value)
}

@ExcludedFromCodegen
internal fun nullable_contref_intrinsic(): contref0? {
    implementedAsIntrinsic
}

@PublishedApi
@Suppress("UNCHECKED_CAST")
internal suspend fun <T> suspendCoroutineUninterceptedOrReturnImpl(block: (Continuation<T>) -> Any?): T {
    val completion = getContinuation<Any?>()
    val wasmCont = WasmContinuation<T, Any?>(WasmContinuationBox(nullable_contref_intrinsic()), completion, synthetic = true)
    wasmCont.isFreshInstance = false
    val result = block(wasmCont)
    if (result === COROUTINE_SUSPENDED) {
        completion.context[SuspensionMarker]?.wasSuspended = true
    }
    if (wasmCont.syntheticResumeValue != null) {
        val resumeResult = wasmCont.syntheticResumeValue!!.getOrThrow()
        wasmCont.syntheticResumeValue = null
        return resumeResult
    }
    if (result !== COROUTINE_SUSPENDED) return result as T
    suspendIntrinsic(wasmCont)
    return wasmCont.result as T
}

@UsedFromCompilerGeneratedCode
@PublishedApi
@Suppress("UNUSED_PARAMETER")
@ExcludedFromCodegen
internal fun suspendIntrinsic(value: Any?) {
    implementedAsIntrinsic
}

private fun <R> resumeWasmContinuationAndReturnResult(contref: contref0, completion: Continuation<R>): Any? {
    val wasmContinuation = WasmContinuation<Continuation<R>, R>(WasmContinuationBox(contref), completion, rethrowExceptions = true)
    wasmContinuation.resume(completion)
    // Check if suspension was marked in the completion's context
    val wasSuspendedInMarker = completion.context[SuspensionMarker]?.wasSuspended == true
    completion.context[SuspensionMarker]?.wasSuspended = false
    if (wasmContinuation.wasSuspended || wasSuspendedInMarker) return COROUTINE_SUSPENDED
    return wasmContinuation.result
}

internal fun <T> suspendFunction0ToContrefImpl(f: (suspend () -> T), completion: Continuation<T>): contref0 {
    return suspendFunction0ToContref(f, completion)
}

internal fun <R, T> suspendFunction1ToContrefImpl(f: (suspend R.() -> T), receiver: R, completion: Continuation<T>): contref0 {
    return suspendFunction1ToContref(f, receiver, completion)
}

internal fun <R, P, T> suspendFunction2ToContrefImpl(f: (suspend R.(P) -> T), receiver: R, param: P, completion: Continuation<T>): contref0 {
    return suspendFunction2ToContref(f, receiver, param, completion)
}

@Suppress("UNUSED_PARAMETER")
@ExcludedFromCodegen
@PublishedApi
internal fun <T> suspendFunction0ToContref(f: (suspend () -> T), completion: Continuation<T>): contref0 {
    implementedAsIntrinsic
}

@Suppress("UNUSED_PARAMETER")
@ExcludedFromCodegen
@PublishedApi
internal fun <R, T> suspendFunction1ToContref(f: (suspend R.() -> T), receiver: R, completion: Continuation<T>): contref0 {
    implementedAsIntrinsic
}

@Suppress("UNUSED_PARAMETER")
@ExcludedFromCodegen
@PublishedApi
internal fun <R, P, T> suspendFunction2ToContref(f: (suspend R.(P) -> T), receiver: R, param: P, completion: Continuation<T>): contref0 {
    implementedAsIntrinsic
}

@PublishedApi
@Suppress("UNCHECKED_CAST")
internal fun <T> startCoroutineUninterceptedOrReturn0Impl(
    f: (suspend () -> T),
    completion: Continuation<T>,
): Any? {
    return resumeWasmContinuationAndReturnResult(suspendFunction0ToContrefImpl(f, completion), completion)
}

@PublishedApi
@Suppress("UNCHECKED_CAST")
internal fun <R, T> startCoroutineUninterceptedOrReturn1Impl(
    f: (suspend R.() -> T),
    receiver: R,
    completion: Continuation<T>,
): Any? {
    return resumeWasmContinuationAndReturnResult(suspendFunction1ToContrefImpl(f, receiver, completion), completion)
}

@PublishedApi
@Suppress("UNCHECKED_CAST")
internal fun <R, P, T> startCoroutineUninterceptedOrReturn2Impl(
    f: (suspend R.(P) -> T),
    receiver: R,
    param: P,
    completion: Continuation<T>,
): Any? {
    return resumeWasmContinuationAndReturnResult(suspendFunction2ToContrefImpl(f, receiver, param, completion), completion)
}

@PublishedApi
@SinceKotlin("1.3")
internal val EmptyContinuation: Continuation<Any?> = Continuation(EmptyCoroutineContext) { result ->
    val _ = result.getOrThrow()
}
