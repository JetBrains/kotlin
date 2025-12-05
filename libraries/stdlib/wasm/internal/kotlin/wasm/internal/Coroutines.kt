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

internal class WasmContinuation<in T>(
    internal var wasmContBox: WasmContinuationBox?,
    private var isResumed: Boolean,
    private val completion: Continuation<*>,
) : Continuation<T> {
    override val context: CoroutineContext = completion.context

    override fun resumeWith(result: Result<T>) {
        if (isResumed) error("Continuation is already resumed")
        wasmContBox?.let { contBox ->
            isResumed = true
            try {
                result.exceptionOrNull()?.let {
                    resumeThrowIntrinsic(it, contBox.cont)
                    return
                }
                val (newCont, wasmContBox) = resumeWithImpl(contBox.cont, result, completion)
                    ?: error("TODO: this is the case where coroutine doesn't suspend and returns the result. We need to handle it by resuming completion with the returned value")
                newCont.wasmContBox = wasmContBox
            } catch (e: Throwable) {
                completion.resumeWithException(e)
            }
        } ?: error("Continuation is not set")
    }
}

internal fun resumeWithImpl(wasmContinuation: contref1, result: Result<*>, completion: Continuation<*>): Pair<WasmContinuation<*>, WasmContinuationBox>? {
    return resumeWithIntrinsic(wasmContinuation, result, completion)
}

@Suppress("UNUSED")
internal fun buildPair(a: Any?, b: contref1) = Pair(a as WasmContinuation<*>, WasmContinuationBox(b))

@Suppress("UNUSED")
internal fun setWasmContinuation(a: Any?, b: contref1): Any? {
    val cont = a as WasmContinuation<*>
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

@Suppress("UNUSED_PARAMETER")
@ExcludedFromCodegen
internal fun resumeWithIntrinsic(wasmContinuation: contref1, result: Result<*>, completion: Continuation<*>): Pair<WasmContinuation<*>, WasmContinuationBox>? {
    implementedAsIntrinsic
}

@PublishedApi
@Suppress("UNCHECKED_CAST")
internal suspend fun <T> suspendCoroutineUninterceptedOrReturnImpl(block: (Continuation<T>) -> Any?): T {
    val completion: Continuation<T> = getContinuation<T>()
    val remainingFunction = WasmContinuation<T>(null, false, completion)
    val result = block(remainingFunction)
    // TODO: completion is never resumed
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

@UsedFromCompilerGeneratedCode
@PublishedApi
@Suppress("UNUSED_PARAMETER")
@ExcludedFromCodegen
internal fun resumeThrowIntrinsic(objectToThrow: Throwable, cont: contref1) {
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

@PublishedApi
internal fun <T> startCoroutineUninterceptedOrReturn0Impl(
    f: (suspend () -> T),
    completion: Continuation<T>
): Any? {
    return startCoroutineUninterceptedOrReturnIntrinsic0Stub(f, completion)
}

@Suppress("UNUSED_PARAMETER")
@ExcludedFromCodegen
@PublishedApi
internal fun <T> startCoroutineUninterceptedOrReturnIntrinsic0Stub(
    f: (suspend () -> T),
    completion: Continuation<T>
): Any? {
    implementedAsIntrinsic
}

@PublishedApi
internal fun <R, T> startCoroutineUninterceptedOrReturn1Impl(
    f: (suspend R.() -> T),
    receiver: R,
    completion: Continuation<T>
): Any? {
    return startCoroutineUninterceptedOrReturnIntrinsic1Stub(receiver, f, completion)
}

@Suppress("UNUSED_PARAMETER")
@ExcludedFromCodegen
@PublishedApi
internal fun <R, T> startCoroutineUninterceptedOrReturnIntrinsic1Stub(
    receiver: R,
    f: (suspend R.() -> T),
    completion: Continuation<T>
): Any? {
    implementedAsIntrinsic
}

@PublishedApi
internal fun <R, P, T> startCoroutineUninterceptedOrReturn2Impl(
    f: (suspend R.(P) -> T),
    receiver: R,
    param: P,
    completion: Continuation<T>
): Any? {
    return startCoroutineUninterceptedOrReturnIntrinsic2Stub(receiver, param, f, completion)
}

@Suppress("UNUSED_PARAMETER")
@ExcludedFromCodegen
@PublishedApi
internal fun <R, P, T> startCoroutineUninterceptedOrReturnIntrinsic2Stub(
    receiver: R,
    param: P,
    f: (suspend R.(P) -> T),
    completion: Continuation<T>
): Any? {
    implementedAsIntrinsic
}

@PublishedApi
@SinceKotlin("1.3")
internal val EmptyContinuation: Continuation<Any?> = Continuation(EmptyCoroutineContext) { result ->
    val _ = result.getOrThrow()
}
