/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:WasmCoroutineMode(isStackSwitchingMode = false)

package kotlin.coroutines.intrinsics

import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineImplStateMachine
import kotlin.internal.UsedFromCompilerGeneratedCode
import kotlin.wasm.internal.WasmCoroutineMode

// Is replaced by Stack Switching intrinsic when -Xwasm-coroutines-stack-switching passed
@UsedFromCompilerGeneratedCode
internal fun <T> createCoroutineUninterceptedIntrinsic0(
    f: suspend () -> T,
    completion: Continuation<T>
): Continuation<Unit> = createCoroutineFromSuspendFunction(completion) {
    f.startCoroutineUninterceptedOrReturn(completion)
}

// Is replaced by Stack Switching intrinsic when -Xwasm-coroutines-stack-switching passed
@UsedFromCompilerGeneratedCode
internal fun <R, T> createCoroutineUninterceptedIntrinsic1(
    f: suspend R.() -> T,
    receiver: R,
    completion: Continuation<T>
): Continuation<Unit> = createCoroutineFromSuspendFunction(completion) {
    f.startCoroutineUninterceptedOrReturn(receiver, completion)
}

// Is not used by Stack Switching Implementation
@Suppress("UNCHECKED_CAST")
private inline fun <T> createCoroutineFromSuspendFunction(
    completion: Continuation<T>,
    crossinline block: () -> Any?
): Continuation<Unit> {
    return object : CoroutineImplStateMachine(completion as Continuation<Any?>) {
        override fun doResume(): Any? {
            exception?.let { throw it }
            return block()
        }
    }
}

// Is not used by Stack Switching implementation
@Suppress("UNCHECKED_CAST")
@UsedFromCompilerGeneratedCode
internal fun <T> createSimpleCoroutineFromSuspendFunction(
    completion: Continuation<T>
): CoroutineImplStateMachine = object : CoroutineImplStateMachine(completion as Continuation<Any?>) {
    override fun doResume(): Any? {
        if (exception != null) throw exception as Throwable
        return result
    }
}

