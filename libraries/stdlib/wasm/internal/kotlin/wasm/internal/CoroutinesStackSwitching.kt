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

@UsedFromCompilerGeneratedCode
@Suppress("UNCHECKED_CAST", "RedundantSuspendModifier")
internal suspend fun <T> suspendCoroutineUninterceptedOrReturnIntrinsicStackSwitching(block: (Continuation<T>) -> Any?): T {
    val coroutineImpl = getContinuation<T>() as CoroutineImplStackSwitching<T, T>

    val blockResult = block(coroutineImpl)
    if (blockResult !== COROUTINE_SUSPENDED) return blockResult as T

    if (coroutineImpl.resumedWhileRunning) {
        // `block` resumed the continuation itself, the result is already here -- do not park.
        coroutineImpl.resumedWhileRunning = false
    } else {
        coroutineImpl.isRunning = false
        suspendIntrinsic(coroutineImpl.wasmContBox)
    }

    coroutineImpl.exception?.let { throw it }
    return coroutineImpl.result as T
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
