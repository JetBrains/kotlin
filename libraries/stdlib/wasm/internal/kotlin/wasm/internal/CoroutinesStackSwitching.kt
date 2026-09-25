/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:WasmCoroutineMode(isStackSwitchingMode = true)

package kotlin.wasm.internal

import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineImplStackSwitching
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.internal.UsedFromCompilerGeneratedCode
import kotlin.wasm.internal.reftypes.typedcontref

// Resumes the execution of `wasmContinuation` by calling wasm `resume` instruction.
// If the continuation suspends, evaluates to the new contref.
// If the continuation completes without suspending, executes wasm `return` with its result from the *calling* function,
// so the caller must return `Any?` and must not have any `try`/`finally` around the call.
@Suppress("UNUSED_PARAMETER")
@ExcludedFromCodegen
internal fun resumeWithIntrinsic(wasmContinuation: typedcontref<(Any?) -> Unit>): typedcontref<(Any?) -> Unit> {
    implementedAsIntrinsic
}

// Same as `resumeIntrinsic`, but calls wasm `resume_throw` instruction
// raising `exception` at the point `wasmContinuation` was suspended previously.
@Suppress("UNUSED_PARAMETER")
@ExcludedFromCodegen
internal fun resumeThrowIntrinsic(exception: Throwable, wasmContinuation: typedcontref<(Any?) -> Unit>): typedcontref<(Any?) -> Unit> {
    implementedAsIntrinsic
}

@ExcludedFromCodegen
internal fun nullContrefIntrinsic(): typedcontref<(Any?) -> Unit>? {
    implementedAsIntrinsic
}

// Replaces `suspendOrReturn` when -Xwasm-use-stack-switching-proposal passed
@Suppress("UNCHECKED_CAST", "RedundantSuspendModifier")
@UsedFromCompilerGeneratedCode
internal suspend fun <T> suspendOrReturnStackSwitching(result: Any?): T {
    val coroutineImpl = getContinuation<T>() as CoroutineImplStackSwitching<T, T>

    if (result !== COROUTINE_SUSPENDED) return result as T

    if (coroutineImpl.resumedWhileRunning) {
        // `block` resumed the continuation itself, the result is already here -- do not park.
        coroutineImpl.resumedWhileRunning = false
        coroutineImpl.exception?.let { throw it }
    } else {
        coroutineImpl.isRunning = false
        suspendIntrinsic()
    }

    return coroutineImpl.result as T
}

@UsedFromCompilerGeneratedCode
@ExcludedFromCodegen
internal fun suspendIntrinsic() {
    implementedAsIntrinsic
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
