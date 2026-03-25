/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.coroutines.intrinsics

import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineImplStackSwitching
import kotlin.coroutines.WasmContinuation
import kotlin.internal.UsedFromCompilerGeneratedCode
import kotlin.wasm.internal.*

@UsedFromCompilerGeneratedCode
internal fun <T> createCoroutineUninterceptedIntrinsic0StackSwitching(
    f: suspend () -> T,
    completion: Continuation<T>
): Continuation<Unit> = WasmContinuation<Unit, T>(
    suspendFunction0ToContrefImpl(f),
    createSimpleCoroutineFromSuspendFunctionStackSwitching(completion)
)

@UsedFromCompilerGeneratedCode
internal fun <R, T> createCoroutineUninterceptedIntrinsic1StackSwitching(
    f: suspend R.() -> T,
    receiver: R,
    completion: Continuation<T>
): Continuation<Unit> = WasmContinuation<Unit, T>(
    suspendFunction1ToContrefImpl(f, receiver),
    createSimpleCoroutineFromSuspendFunctionStackSwitching(completion)
)

@Suppress("UNCHECKED_CAST")
@UsedFromCompilerGeneratedCode
internal fun <T> createSimpleCoroutineFromSuspendFunctionStackSwitching(
    completion: Continuation<T>
): CoroutineImplStackSwitching<Any?, T> = object : CoroutineImplStackSwitching<Any?, T>(completion) {
    override fun doResume(): Any? {
        if (exception != null) throw exception as Throwable
        return result
    }
}

