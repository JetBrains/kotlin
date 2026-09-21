/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:WasmCoroutineMode(isStackSwitchingMode = true)

package kotlin.coroutines

import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.internal.UsedFromCompilerGeneratedCode
import kotlin.wasm.internal.WasmPrimitiveConstructor
import kotlin.wasm.internal.WasmCoroutineMode
import kotlin.wasm.internal.nullContrefIntrinsic
import kotlin.wasm.internal.reftypes.typedcontref
import kotlin.wasm.internal.resumeThrowImpl
import kotlin.wasm.internal.resumeWithImpl


@SinceKotlin("1.3")
@UsedFromCompilerGeneratedCode
internal class CoroutineImplStackSwitching<T, R>(
    private val resultContinuation: Continuation<R>,
    internal val wasmContBox: WasmContinuationBox =
        WasmContinuationBox(nullContrefIntrinsic())
) : Continuation<T> {

    internal var result: Any? = null
    internal var exception: Throwable? = null

    public override val context: CoroutineContext = resultContinuation.context
    private var intercepted_: Continuation<T>? = null
    public fun intercepted(): Continuation<T> = intercepted_
        ?: (context[ContinuationInterceptor]?.interceptContinuation(this) ?: this)
            .also { intercepted_ = it }

    internal var pendingSuspend = false

    // True while this coroutine's wasm stack is executing
    internal var isRunning = true

    // Set by `resumeWith` when the coroutine resumes itself from inside its own `block`.
    internal var resumedWhileRunning = false

    @Suppress("UNCHECKED_CAST")
    override fun resumeWith(result: Result<T>) {
        this.result = result.getOrNull()
        exception = result.exceptionOrNull()

        // The coroutine is resuming itself from inside its own `block`: the wasm stack is still
        // running, so there is nothing to resume -- just park the result for the block to pick up.
        if (isRunning) {
            check(!resumedWhileRunning) { "Continuation was resumed more than once" }
            resumedWhileRunning = true
            return
        }

        try {
            val outcome = doResume()
            this.result = outcome
            exception = null
            if (outcome === COROUTINE_SUSPENDED) return
        } catch (exception: Throwable) { // Catch all exceptions
            this.result = null
            this.exception = exception
        }
        isRunning = false // the stack ran to completion

        releaseIntercepted() // this instance is terminating

        val completion = resultContinuation

        // top-level completion reached -- invoke and return
        if (exception != null) {
            completion.resumeWithException(exception!!)
        } else {
            completion.resume(this.result as R)
        }
    }

    private fun releaseIntercepted() {
        val intercepted = intercepted_
        if (intercepted != null && intercepted !== this) {
            context[ContinuationInterceptor]!!.releaseInterceptedContinuation(intercepted)
        }
        this.intercepted_ = CompletedContinuation // just in case
    }

    fun doResume(): Any? {
        val wasmCont = wasmContBox.wasmContinuation!!
        wasmContBox.wasmContinuation = nullContrefIntrinsic()
        isRunning = true

        val e = exception
        val resumeResult: Any? =
            if (e != null)
                resumeThrowImpl(e, wasmCont)
            else
                resumeWithImpl(wasmCont)

        return resumeResult
    }
}

internal class WasmContinuationBox @WasmPrimitiveConstructor constructor(var wasmContinuation: typedcontref<(Any?) -> Unit>?)
