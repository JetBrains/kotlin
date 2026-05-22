/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:WasmCoroutineMode(isStackSwitchingMode = true)

package kotlin.coroutines.intrinsics

import kotlin.coroutines.Continuation
import kotlin.coroutines.WasmContinuation
import kotlin.internal.UsedFromCompilerGeneratedCode
import kotlin.wasm.internal.*
import kotlin.wasm.internal.suspendFunction0ToContrefImpl

@UsedFromCompilerGeneratedCode
internal fun <T> createCoroutineUninterceptedIntrinsic0StackSwitching(
    f: suspend () -> T,
    completion: Continuation<T>
): Continuation<Unit> = WasmContinuation<Unit, T>(
    suspendFunction0ToContrefImpl(f),
    completion
)

@UsedFromCompilerGeneratedCode
internal fun <R, T> createCoroutineUninterceptedIntrinsic1StackSwitching(
    f: suspend R.() -> T,
    receiver: R,
    completion: Continuation<T>
): Continuation<Unit> = WasmContinuation<Unit, T>(
    suspendFunction1ToContrefImpl(f, receiver),
    completion
)
