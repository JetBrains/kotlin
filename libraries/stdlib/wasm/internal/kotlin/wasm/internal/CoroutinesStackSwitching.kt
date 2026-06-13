/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:WasmCoroutineMode(isStackSwitchingMode = true)

package kotlin.wasm.internal

import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineImplStackSwitching
import kotlin.coroutines.WasmContinuationBox
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.internal.DoNotInlineOnFirstStage
import kotlin.internal.UsedFromCompilerGeneratedCode
import kotlin.wasm.internal.reftypes.typedcontref

// Resumes the execution of wasm contref (wasmContinuation parameter)
// by calling wasm `resume` instruction.
//
// When the execution suspends, returns COROUTINE_SUSPENDED.
// If the suspension doesn't happen, returns the result.
@Suppress("UNUSED_PARAMETER")
internal fun resumeWithImpl(wasmContinuation: typedcontref<(Any?) -> Unit>): Any? =
    resumeWithIntrinsic()

// Resumes the execution of wasm contref (wasmContinuation parameter)
// by calling wasm `resume_throw` instruction.
// It raises an exception (`exceptionToResume`) at the point contref was suspended previously
// (after `suspend` instruction).
//
// When the execution suspends, returns COROUTINE_SUSPENDED.
// If the suspension doesn't happen, returns the result.
@Suppress("UNUSED_PARAMETER")
internal fun resumeThrowImpl(exceptionToResume: Throwable, wasmContinuation: typedcontref<(Any?) -> Unit>): Any? =
    resumeThrowIntrinsic()

@ExcludedFromCodegen
internal fun resumeWithIntrinsic(): Any? {
    implementedAsIntrinsic
}

@ExcludedFromCodegen
internal fun resumeThrowIntrinsic(): Any? {
    implementedAsIntrinsic
}

@ExcludedFromCodegen
internal fun nullContrefIntrinsic(): typedcontref<(Any?) -> Unit>? {
    implementedAsIntrinsic
}

internal suspend fun <T> getBlockKotlinContinuation(): CoroutineImplStackSwitching<T, T> {
    val completion = getContinuation<T>()
    val wasmContBox = WasmContinuationBox(nullContrefIntrinsic())
    val blockKotlinContinuation = CoroutineImplStackSwitching<T, T>(completion, wasmContBox)
    blockKotlinContinuation.pendingSuspend = true
    return blockKotlinContinuation
}

@Suppress("UNCHECKED_CAST")
internal fun <T> getBlockKotlinContinuationResult(blockKotlinContinuation: CoroutineImplStackSwitching<T, T>): T {
    val e = blockKotlinContinuation.exception
    if (e != null) throw e
    return blockKotlinContinuation.result as T
}

internal fun <T> checkNotPendingSuspend(blockKotlinContinuation: CoroutineImplStackSwitching<T, T>) {
    if (blockKotlinContinuation.pendingSuspend) {
        blockKotlinContinuation.pendingSuspend = false
        suspendIntrinsic(blockKotlinContinuation.wasmContBox)
    }
}

// Uses internal non-inlined internal API:
// getBlockKotlinContinuation,
// checkNotPendingSuspend,
// getBlockKotlinContinuationResult
@DoNotInlineOnFirstStage
@UsedFromCompilerGeneratedCode
@Suppress("UNCHECKED_CAST")
internal suspend inline fun <T> suspendCoroutineUninterceptedOrReturnStackSwitching(block: (Continuation<T>) -> Any?): T {
    val blockKotlinContinuation = getBlockKotlinContinuation<T>()

    val blockResult = block(blockKotlinContinuation)
    if (blockResult !== COROUTINE_SUSPENDED) return blockResult as T

    checkNotPendingSuspend(blockKotlinContinuation)

    return getBlockKotlinContinuationResult(blockKotlinContinuation)
}

@Suppress("UNUSED_PARAMETER")
@UsedFromCompilerGeneratedCode
@ExcludedFromCodegen
internal fun suspendIntrinsic(contBox: WasmContinuationBox) {
    implementedAsIntrinsic
}

@UsedFromCompilerGeneratedCode
internal fun <T> suspendFunction0ToContrefImpl(f: (suspend () -> T), completion: Continuation<T>): typedcontref<(Any?) -> Unit> {
    return suspendFunction0ToContref(f, completion)
}

@UsedFromCompilerGeneratedCode
internal fun <R, T> suspendFunction1ToContrefImpl(
    f: (suspend R.() -> T),
    receiver: R,
    completion: Continuation<T>
): typedcontref<(Any?) -> Unit> {
    return suspendFunction1ToContref(f, receiver, completion)
}

@UsedFromCompilerGeneratedCode
internal fun <R, P, T> suspendFunction2ToContrefImpl(
    f: (suspend R.(P) -> T),
    receiver: R,
    param: P,
    completion: Continuation<T>
): typedcontref<(Any?) -> Unit> {
    return suspendFunction2ToContref(f, receiver, param, completion)
}

@Suppress("UNUSED_PARAMETER")
@ExcludedFromCodegen
internal fun <T> suspendFunction0ToContref(f: (suspend () -> T), completion: Continuation<T>): typedcontref<(Any?) -> Unit> {
    implementedAsIntrinsic
}

@Suppress("UNUSED_PARAMETER")
@ExcludedFromCodegen
internal fun <R, T> suspendFunction1ToContref(
    f: (suspend R.() -> T),
    receiver: R,
    completion: Continuation<T>
): typedcontref<(Any?) -> Unit> {
    implementedAsIntrinsic
}

@Suppress("UNUSED_PARAMETER")
@ExcludedFromCodegen
internal fun <R, P, T> suspendFunction2ToContref(
    f: (suspend R.(P) -> T),
    receiver: R,
    param: P,
    completion: Continuation<T>
): typedcontref<(Any?) -> Unit> {
    implementedAsIntrinsic
}
