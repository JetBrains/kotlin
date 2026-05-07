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

@Suppress("UNUSED_PARAMETER")
internal fun resumeWithImpl(wasmContinuation: typedcontref<() -> Any?>): Any? =
    resumeWithIntrinsic()

@Suppress("UNUSED_PARAMETER")
internal fun resumeThrowImpl(objectToThrow: Throwable, cont: typedcontref<() -> Any?>): Any? =
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
internal fun nullableContrefIntrinsic(): typedcontref<() -> Any?>? {
    implementedAsIntrinsic
}

@DoNotInlineOnFirstStage
@UsedFromCompilerGeneratedCode
@Suppress("UNCHECKED_CAST")
internal suspend inline fun <T> suspendCoroutineUninterceptedOrReturnStackSwitching(block: (Continuation<T>) -> Any?): T {
    val completion = getContinuation<T>()
    val wasmContBox = WasmContinuationBox(nullableContrefIntrinsic(), false)
    val freshCont = CoroutineImplStackSwitching<T, T>(completion, wasmContBox)
    wasmContBox.pendingSuspend = true
    val blockResult = block(freshCont)

    if (blockResult !== COROUTINE_SUSPENDED) return blockResult as T

    if (freshCont.wasmContBox.pendingSuspend) {
        freshCont.wasmContBox.pendingSuspend = false
        suspendIntrinsic(freshCont.wasmContBox)
    }

    val e = freshCont.exception
    if (e != null) throw e
    return freshCont.result as T
}

@Suppress("UNUSED_PARAMETER")
@UsedFromCompilerGeneratedCode
@ExcludedFromCodegen
internal fun suspendIntrinsic(contBox: WasmContinuationBox) {
    implementedAsIntrinsic
}

@UsedFromCompilerGeneratedCode
internal fun <T> suspendFunction0ToContrefImpl(f: (suspend () -> T), completion: Continuation<T>): typedcontref<() -> Any?> {
    return suspendFunction0ToContref(f, completion)
}

@UsedFromCompilerGeneratedCode
internal fun <R, T> suspendFunction1ToContrefImpl(
    f: (suspend R.() -> T),
    receiver: R,
    completion: Continuation<T>
): typedcontref<() -> Any?> {
    return suspendFunction1ToContref(f, receiver, completion)
}

@UsedFromCompilerGeneratedCode
internal fun <R, P, T> suspendFunction2ToContrefImpl(
    f: (suspend R.(P) -> T),
    receiver: R,
    param: P,
    completion: Continuation<T>
): typedcontref<() -> Any?> {
    return suspendFunction2ToContref(f, receiver, param, completion)
}

@Suppress("UNUSED_PARAMETER")
@ExcludedFromCodegen
internal fun <T> suspendFunction0ToContref(f: (suspend () -> T), completion: Continuation<T>): typedcontref<() -> Any?> {
    implementedAsIntrinsic
}

@Suppress("UNUSED_PARAMETER")
@ExcludedFromCodegen
internal fun <R, T> suspendFunction1ToContref(f: (suspend R.() -> T), receiver: R, completion: Continuation<T>): typedcontref<() -> Any?> {
    implementedAsIntrinsic
}

@Suppress("UNUSED_PARAMETER")
@ExcludedFromCodegen
internal fun <R, P, T> suspendFunction2ToContref(
    f: (suspend R.(P) -> T),
    receiver: R,
    param: P,
    completion: Continuation<T>
): typedcontref<() -> Any?> {
    implementedAsIntrinsic
}
