/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:WasmCoroutineMode(isStackSwitchingMode = false)

package kotlin.coroutines.intrinsics

import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineImpl
import kotlin.internal.UsedFromCompilerGeneratedCode
import kotlin.wasm.internal.WasmCoroutineMode

// Intrinsics `startCoroutine...` use SuspendFunctionN <: Function{N + 1} conversion to
// run coroutine in non-suspend context.
@Suppress("UNCHECKED_CAST")
@PublishedApi
@UsedFromCompilerGeneratedCode
internal fun <T> (suspend () -> T).startCoroutineUninterceptedOrReturnImpl(
    completion: Continuation<T>
): Any? =
    (this as Function1<Continuation<T>, Any?>)(wrapCompletion(completion))

@Suppress("UNCHECKED_CAST")
@PublishedApi
@UsedFromCompilerGeneratedCode
internal fun <R, T> (suspend R.() -> T).startCoroutineUninterceptedOrReturnImpl(
    receiver: R,
    completion: Continuation<T>
): Any? =
    (this as Function2<R, Continuation<T>, Any?>)(receiver, wrapCompletion(completion))

@Suppress("UNCHECKED_CAST")
@UsedFromCompilerGeneratedCode
internal fun <R, P, T> (suspend R.(P) -> T).startCoroutineUninterceptedOrReturnImpl(
    receiver: R,
    param: P,
    completion: Continuation<T>
): Any? =
    (this as Function3<R, P, Continuation<T>, Any?>)(receiver, param, wrapCompletion(completion))

// Each suspend lambda is already a CoroutineImpl - no wrapping happens.
// Suspend callable references, user classes implementing SuspendFunction, etc. are not CoroutineImpl - wrap them.
// Initially implemented to fix the `intercepted` box tests KT-55869.
@Suppress("NOTHING_TO_INLINE")
internal inline fun <T> Any?.wrapCompletion(completion: Continuation<T>): Continuation<T> =
    if (this is CoroutineImpl) completion
    else createSimpleCoroutine(completion)

// Is not used by Stack Switching implementation
@Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
@UsedFromCompilerGeneratedCode
internal inline fun <T> createSimpleCoroutine(
    completion: Continuation<T>
): CoroutineImpl = object : CoroutineImpl(completion as Continuation<Any?>) {
    override fun doResume(): Any? {
        if (exception != null) throw exception as Throwable
        return result
    }
}

// Is replaced by Stack Switching intrinsic when -Xwasm-use-stack-switching-proposal passed
@UsedFromCompilerGeneratedCode
internal fun <T> createCoroutineUninterceptedIntrinsic0(
    f: suspend () -> T,
    completion: Continuation<T>
): Continuation<Unit> = createCoroutineFromSuspendFunction(completion) {
    f.startCoroutineUninterceptedOrReturn(completion)
}

// Is replaced by Stack Switching intrinsic when -Xwasm-use-stack-switching-proposal passed
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
    return object : CoroutineImpl(completion as Continuation<Any?>) {
        override fun doResume(): Any? {
            exception?.let { throw it }
            return block()
        }
    }
}
