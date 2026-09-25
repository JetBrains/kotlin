/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:WasmCoroutineMode(isStackSwitchingMode = true)

package kotlin.coroutines.intrinsics

import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineImplStackSwitching
import kotlin.internal.UsedFromCompilerGeneratedCode
import kotlin.wasm.internal.*
import kotlin.wasm.internal.reftypes.typedcontref

// `startCoroutineUninterceptedOrReturn...` run the suspend function on a new wasm stack.
// It provides with the completion wrapped into a CoroutineImplStackSwitching, so that
// its lifetime properties, like `isRunning` and `resumedWhileRunning`, will be visible.
// Also, this way it establishes a correct mechanism for the coroutine interception KT-55869.

// Replaces `startCoroutineUninterceptedOrReturnImpl` when -Xwasm-use-stack-switching-proposal passed
@UsedFromCompilerGeneratedCode
internal fun <T> (suspend () -> T).startCoroutineUninterceptedOrReturnStackSwitchingImpl(
    completion: Continuation<T>
): Any? {
    val wrappedCompletion = CoroutineImplStackSwitching<T, T>(completion)
    val contref0 = suspendFunction0ToContref(this, wrappedCompletion)
    return startCoroutineStackSwitching(contref0, wrappedCompletion)
}

// Replaces `startCoroutineUninterceptedOrReturnImpl` when -Xwasm-use-stack-switching-proposal passed
@UsedFromCompilerGeneratedCode
internal fun <R, T> (suspend R.() -> T).startCoroutineUninterceptedOrReturnStackSwitchingImpl(
    receiver: R,
    completion: Continuation<T>
): Any? {
    val wrappedCompletion = CoroutineImplStackSwitching<T, T>(completion)
    val contref1 = suspendFunction1ToContref(this, receiver, wrappedCompletion)
    return startCoroutineStackSwitching(contref1, wrappedCompletion)
}

// Replaces `startCoroutineUninterceptedOrReturnImpl` when -Xwasm-use-stack-switching-proposal passed
@UsedFromCompilerGeneratedCode
internal fun <R, P, T> (suspend R.(P) -> T).startCoroutineUninterceptedOrReturnStackSwitchingImpl(
    receiver: R,
    param: P,
    completion: Continuation<T>
): Any? {
    val wrappedCompletion = CoroutineImplStackSwitching<T, T>(completion)
    val contref2 = suspendFunction2ToContref(this, receiver, param, wrappedCompletion)
    return startCoroutineStackSwitching(contref2, wrappedCompletion)
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun startCoroutineStackSwitching(
    wasmContinuation: typedcontref<(Any?) -> Unit>,
    completion: CoroutineImplStackSwitching<*, *>,
): Any? {
    val result = resumeWithImpl(wasmContinuation)
    if (result !== COROUTINE_SUSPENDED) completion.isRunning = false
    return result
}

// Replaces `createCoroutineUninterceptedIntrinsic0` when -Xwasm-use-stack-switching-proposal passed
@UsedFromCompilerGeneratedCode
internal fun <T> createCoroutineUninterceptedIntrinsic0StackSwitching(
    f: suspend () -> T,
    completion: Continuation<T>
): Continuation<Unit> = createCoroutineFromSuspendFunctionStackSwitching(completion) {
    f.startCoroutineUninterceptedOrReturn(completion)
}

// Replaces `createCoroutineUninterceptedIntrinsic1` when -Xwasm-use-stack-switching-proposal passed
@UsedFromCompilerGeneratedCode
internal fun <R, T> createCoroutineUninterceptedIntrinsic1StackSwitching(
    f: suspend R.() -> T,
    receiver: R,
    completion: Continuation<T>
): Continuation<Unit> = createCoroutineFromSuspendFunctionStackSwitching(completion) {
    f.startCoroutineUninterceptedOrReturn(receiver, completion)
}

private inline fun <T> createCoroutineFromSuspendFunctionStackSwitching(
    completion: Continuation<T>,
    crossinline block: () -> Any?
): Continuation<Unit> {
    return object : CoroutineImplStackSwitching<Unit, T>(completion) {
        // This coroutine owns no wasm stack of its own -- the body runs under a different
        // continuation, created by generated code -- so it is started by `resumeWith` rather than
        // by generated code, and never becomes "running" itself.
        init {
            isRunning = false
        }

        override fun doResume(): Any? {
            exception?.let { throw it }
            return block()
        }
    }
}
