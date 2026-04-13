/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.coroutines.intrinsics

import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineImpl
//import kotlin.coroutines.CoroutineImplStateMachine
import kotlin.internal.UsedFromCompilerGeneratedCode

@UsedFromCompilerGeneratedCode
internal fun <T> createCoroutineUninterceptedIntrinsic0StateMachine(
    f: suspend () -> T,
    completion: Continuation<T>
): Continuation<Unit> = createSimpleCoroutineFromSuspendFunctionStateMachinePrivate(completion) {
    f.startCoroutineUninterceptedOrReturn(completion)
}

@UsedFromCompilerGeneratedCode
internal fun <R, T> createCoroutineUninterceptedIntrinsic1StateMachine(
    f: suspend R.() -> T,
    receiver: R,
    completion: Continuation<T>
): Continuation<Unit> = createSimpleCoroutineFromSuspendFunctionStateMachinePrivate(completion) {
    f.startCoroutineUninterceptedOrReturn(receiver, completion)
}

@Suppress("UNCHECKED_CAST")
private inline fun <T> createSimpleCoroutineFromSuspendFunctionStateMachinePrivate(
    completion: Continuation<T>,
    crossinline block: () -> Any?
): Continuation<Unit> =
//    object : CoroutineImplStateMachine(completion as Continuation<Any?>) {
    object : CoroutineImpl(completion as Continuation<Any?>) {
        override fun doResume(): Any? {
            exception?.let { throw it }
            return block()
        }
    }

@Suppress("UNCHECKED_CAST")
@UsedFromCompilerGeneratedCode
internal fun <T> createSimpleCoroutineFromSuspendFunctionStateMachine(
    completion: Continuation<T>
//): CoroutineImplStateMachine = object : CoroutineImplStateMachine(completion as Continuation<Any?>) {
): CoroutineImpl = object : CoroutineImpl(completion as Continuation<Any?>) {
    override fun doResume(): Any? {
        if (exception != null) throw exception as Throwable
        return result
    }
}
