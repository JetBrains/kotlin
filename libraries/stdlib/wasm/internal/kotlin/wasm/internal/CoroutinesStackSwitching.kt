/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:WasmStackSwitchingOnly

package kotlin.wasm.internal

import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineImplStackSwitching
import kotlin.coroutines.WasmContinuation
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
    val suspendBody: ((Continuation<*>) -> Any?)?,
    val remainingFunction: contref1?,
    val result: Any?,
)

@Suppress("UNUSED")
@UsedFromCompilerGeneratedCode
internal fun buildResumeIntrinsicSuspendResult(
    suspendBody: ((Continuation<*>) -> Any?)?,
    remainingFunction: contref1,
): ResumeIntrinsicResult {
    return ResumeIntrinsicResult(suspendBody, remainingFunction, null)
}

@Suppress("UNUSED")
@UsedFromCompilerGeneratedCode
internal fun buildResumeIntrinsicValueResult(value: Any?): ResumeIntrinsicResult {
    return ResumeIntrinsicResult(null, nullableContrefIntrinsic(), value)
}

@ExcludedFromCodegen
internal fun nullableContrefIntrinsic(): contref1? {
    implementedAsIntrinsic
}

@Suppress("UNCHECKED_CAST")
@PublishedApi
@DoNotInlineOnFirstStage
@UsedFromCompilerGeneratedCode
internal suspend fun <T> suspendCoroutineUninterceptedOrReturnStackSwitching(block: (Continuation<T>) -> Any?): T {
    return suspendIntrinsic(block) as T
}

@Suppress("UNUSED_PARAMETER")
@UsedFromCompilerGeneratedCode
@PublishedApi
@ExcludedFromCodegen
internal fun <T> suspendIntrinsic(block: (Continuation<T>) -> Any?): Any? {
    implementedAsIntrinsic
}

@UsedFromCompilerGeneratedCode
internal fun <R> resumeWasmContinuationAndReturnResult(contref: contref1, completion: Continuation<R>): Any? {
    val wasmContinuation = WasmContinuation<Continuation<R>, R>(contref, completion, rethrowExceptions = true)
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
