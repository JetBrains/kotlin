/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:WasmCoroutineMode(isStackSwitchingMode = true)

package kotlin.wasm.internal

import kotlin.coroutines.Continuation
import kotlin.coroutines.WasmContinuation
import kotlin.internal.DoNotInlineOnFirstStage
import kotlin.internal.UsedFromCompilerGeneratedCode
import kotlin.wasm.internal.reftypes.typedcontref

@Suppress("UNUSED_PARAMETER")
internal fun resumeWithImpl(resultValue: Any?, cont: typedcontref<(Any?) -> Any?>): ResumeIntrinsicResult =
    resumeWithIntrinsic()

@Suppress("UNUSED_PARAMETER")
internal fun resumeThrowImpl(objectToThrow: Throwable, cont: typedcontref<(Any?) -> Any?>): ResumeIntrinsicResult =
    resumeThrowIntrinsic()

@ExcludedFromCodegen
internal fun resumeWithIntrinsic(): ResumeIntrinsicResult {
    implementedAsIntrinsic
}

@ExcludedFromCodegen
internal fun resumeThrowIntrinsic(): ResumeIntrinsicResult {
    implementedAsIntrinsic
}

internal class ResumeIntrinsicResult(
    val suspendBody: ((Continuation<*>) -> Any?)?,
    val remainingFunction: typedcontref<(Any?) -> Any?>?,
    val result: Any?,
)

@Suppress("UNUSED")
@UsedFromCompilerGeneratedCode
internal fun buildResumeIntrinsicSuspendResult(
    suspendBody: ((Continuation<*>) -> Any?)?,
    remainingFunction: typedcontref<(Any?) -> Any?>,
): ResumeIntrinsicResult {
    return ResumeIntrinsicResult(suspendBody, remainingFunction, null)
}

@Suppress("UNUSED")
@UsedFromCompilerGeneratedCode
internal fun buildResumeIntrinsicValueResult(value: Any?): ResumeIntrinsicResult {
    return ResumeIntrinsicResult(null, nullableContrefIntrinsic(), value)
}

@ExcludedFromCodegen
internal fun nullableContrefIntrinsic(): typedcontref<(Any?) -> Any?>? {
    implementedAsIntrinsic
}

@DoNotInlineOnFirstStage
@UsedFromCompilerGeneratedCode
@Suppress("UNCHECKED_CAST")
internal suspend fun <T> suspendCoroutineUninterceptedOrReturnStackSwitching(block: (Continuation<T>) -> Any?): T {
    return suspendIntrinsic(block) as T
}

@Suppress("UNUSED_PARAMETER")
@UsedFromCompilerGeneratedCode
@ExcludedFromCodegen
internal fun <T> suspendIntrinsic(block: (Continuation<T>) -> Any?): Any {
    implementedAsIntrinsic
}

@UsedFromCompilerGeneratedCode
internal fun <T> suspendFunction0ToContrefImpl(f: (suspend () -> T)): typedcontref<(Any?) -> Any?> {
    return suspendFunction0ToContref(f)
}

@UsedFromCompilerGeneratedCode
internal fun <R, T> suspendFunction1ToContrefImpl(
    f: (suspend R.() -> T),
    receiver: R
): typedcontref<(Any?) -> Any?> {
    return suspendFunction1ToContref(f, receiver)
}

@UsedFromCompilerGeneratedCode
internal fun <R, P, T> suspendFunction2ToContrefImpl(
    f: (suspend R.(P) -> T),
    receiver: R,
    param: P
): typedcontref<(Any?) -> Any?> {
    return suspendFunction2ToContref(f, receiver, param)
}

@Suppress("UNUSED_PARAMETER")
@ExcludedFromCodegen
internal fun <T> suspendFunction0ToContref(f: (suspend () -> T)): typedcontref<(Any?) -> Any?> {
    implementedAsIntrinsic
}

@Suppress("UNUSED_PARAMETER")
@ExcludedFromCodegen
internal fun <R, T> suspendFunction1ToContref(f: (suspend R.() -> T), receiver: R): typedcontref<(Any?) -> Any?> {
    implementedAsIntrinsic
}

@Suppress("UNUSED_PARAMETER")
@ExcludedFromCodegen
internal fun <R, P, T> suspendFunction2ToContref(
    f: (suspend R.(P) -> T),
    receiver: R,
    param: P
): typedcontref<(Any?) -> Any?> {
    implementedAsIntrinsic
}

@UsedFromCompilerGeneratedCode
@Suppress("UNCHECKED_CAST")
internal fun <T> startCoroutineUninterceptedOrReturnImplStackSwitching(
    completion: Continuation<T>,
    wasmCont: typedcontref<(Any?) -> Any?>
): Any? {
    val wasmContinuation = WasmContinuation<T, T>(wasmCont, completion)
    wasmContinuation.result = completion
    return wasmContinuation.doResume()
}
