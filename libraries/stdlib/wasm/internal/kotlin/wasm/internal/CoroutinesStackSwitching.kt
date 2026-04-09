/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.wasm.internal

import kotlin.coroutines.Continuation
import kotlin.coroutines.WasmContinuation
import kotlin.coroutines.WasmContinuationBox
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.resume
import kotlin.internal.DoNotInlineOnFirstStage
import kotlin.internal.UsedFromCompilerGeneratedCode
import kotlin.wasm.internal.reftypes.contref1

@Suppress("UNUSED_PARAMETER")
internal fun resumeWithImpl(result: Any?, wasmContinuation: contref1): ResumeIntrinsicResult =
    resumeWithIntrinsic()

@Suppress("UNUSED_PARAMETER")
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
    continuation: Any?,
    remainingFunction: contref1,
): ResumeIntrinsicResult {
    (continuation as? WasmContinuationBox)?.wasmContinuation = remainingFunction
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
@DoNotInlineOnFirstStage
@UsedFromCompilerGeneratedCode
@Suppress(
    "USELESS_CAST",
    "UNCHECKED_CAST",
    "RETURN_VALUE_NOT_USED",
    "LESS_VISIBLE_TYPE_ACCESS_IN_INLINE_ERROR",
    "NON_PUBLIC_CALL_FROM_PUBLIC_INLINE"
)
internal suspend inline fun <T> suspendCoroutineUninterceptedOrReturnStackSwitching(block: (Continuation<T>) -> Any?): T {
    val completion = getContinuation<T>() as Continuation<T>

//  actually this code handled correctness tests that aimed to simulate suspension via returning COROUTINE_SUSPENDED
//  such a hack isn't needed
//    (completion as? WasmContinuation<*, *>)?.wasSuspended = true
    val wasmContBox = WasmContinuationBox(nullableContrefIntrinsic())
    val freshCont = WasmContinuation<T, T>(wasmContBox, completion)
    wasmContBox.pendingSuspend = true
    val blockResult = block(freshCont)

    if (blockResult !== COROUTINE_SUSPENDED) return blockResult as T

    // Sync resume: block called freshCont.resumeWith() synchronously, which cleared pendingSuspend.
    if (!wasmContBox.pendingSuspend) {
        val e = freshCont.exception
        if (e != null) throw e
        return freshCont.result as T
    }

    wasmContBox.pendingSuspend = false
    suspendIntrinsic(wasmContBox)
    val e = freshCont.exception
    if (e != null) throw e
    return freshCont.result as T
}

@Suppress("UNUSED_PARAMETER")
@UsedFromCompilerGeneratedCode
@PublishedApi
@ExcludedFromCodegen
internal fun suspendIntrinsic(contBox: WasmContinuationBox?): Any? {
    implementedAsIntrinsic
}

@UsedFromCompilerGeneratedCode
internal fun <R> resumeWasmContinuationAndReturnResult(contref: contref1, completion: Continuation<R>): Any? {
    val wasmContBox = WasmContinuationBox(contref)
    val wasmContinuation = WasmContinuation<Continuation<R>, R>(wasmContBox, completion, rethrowExceptions = true)
    wasmContinuation.resume(completion)
    return if (wasmContinuation.wasSuspended) COROUTINE_SUSPENDED else wasmContinuation.result
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
