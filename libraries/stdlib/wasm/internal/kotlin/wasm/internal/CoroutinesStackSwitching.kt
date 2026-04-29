/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:WasmStackSwitchingOnly

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
internal fun resumeWithImpl(result: Any?, wasmContinuation: contref1): Any? =
    resumeWithIntrinsic()

@Suppress("UNUSED_PARAMETER")
internal fun resumeThrowImpl(objectToThrow: Throwable, cont: contref1): Any? =
    resumeThrowIntrinsic()

@ExcludedFromCodegen
internal fun resumeWithIntrinsic(): Any? {
    implementedAsIntrinsic
}

@ExcludedFromCodegen
internal fun resumeThrowIntrinsic(): Any? {
    implementedAsIntrinsic
}

@UsedFromCompilerGeneratedCode
internal fun buildResumeIntrinsicSuspendResult(
    continuation: Any,
    remainingFunction: contref1,
): Any {
    wasm_ref_cast_null<WasmContinuationBox>(continuation).wasmContinuation = remainingFunction
    return COROUTINE_SUSPENDED
}

@ExcludedFromCodegen
internal fun nullableContrefIntrinsic(): contref1? {
    implementedAsIntrinsic
}

@PublishedApi
internal suspend fun <T> getWasmCont(): WasmContinuation<T, T> {
    val completion = getContinuation<T>()
    val wasmContBox = WasmContinuationBox(nullableContrefIntrinsic(), false)
    val freshCont = WasmContinuation<T, T>(wasmContBox, completion)
    wasmContBox.pendingSuspend = true
    return freshCont
}

@Suppress("UNCHECKED_CAST")
@PublishedApi
internal fun <T> getWasmContResult(wasmCont: WasmContinuation<T, T>): T {
    val e = wasmCont.exception
    if (e != null) throw e
    return wasmCont.result as T
}

@PublishedApi
internal fun <T> checkNotPendingSuspend(wasmCont: WasmContinuation<T, T>) {
    if (wasmCont.wasmContBox.pendingSuspend) {
        wasmCont.wasmContBox.pendingSuspend = false
        suspendIntrinsic(wasmCont.wasmContBox)
    }
}

@PublishedApi
@DoNotInlineOnFirstStage
@UsedFromCompilerGeneratedCode
@Suppress("UNCHECKED_CAST")
internal suspend inline fun <T> suspendCoroutineUninterceptedOrReturnStackSwitching(block: (Continuation<T>) -> Any?): T {
    val freshCont = getWasmCont<T>()
    val blockResult = block(freshCont)

    if (blockResult !== COROUTINE_SUSPENDED) return blockResult as T

    checkNotPendingSuspend(freshCont)

    return getWasmContResult(freshCont)
}

@Suppress("UNUSED_PARAMETER")
@UsedFromCompilerGeneratedCode
@ExcludedFromCodegen
internal fun suspendIntrinsic(contBox: WasmContinuationBox) {
    implementedAsIntrinsic
}

@UsedFromCompilerGeneratedCode
internal fun <R> resumeWasmContinuationAndReturnResult(contref: contref1, completion: Continuation<R>): Any? {
    val wasmContinuation = WasmContinuation<Continuation<R>, R>(WasmContinuationBox(contref, false), completion, rethrowExceptions = true)
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
internal fun <T> suspendFunction0ToContref(f: (suspend () -> T)): contref1 {
    implementedAsIntrinsic
}

@Suppress("UNUSED_PARAMETER")
@ExcludedFromCodegen
internal fun <R, T> suspendFunction1ToContref(f: (suspend R.() -> T), receiver: R): contref1 {
    implementedAsIntrinsic
}

@Suppress("UNUSED_PARAMETER")
@ExcludedFromCodegen
internal fun <R, P, T> suspendFunction2ToContref(f: (suspend R.(P) -> T), receiver: R, param: P): contref1 {
    implementedAsIntrinsic
}
