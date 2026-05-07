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
import kotlin.wasm.internal.nullableContrefIntrinsic
import kotlin.wasm.internal.reftypes.contref1
import kotlin.wasm.internal.resumeThrowImpl
import kotlin.wasm.internal.resumeWithImpl


@SinceKotlin("1.3")
@UsedFromCompilerGeneratedCode
internal class CoroutineImplStackSwitching<T, R>(
    resultContinuation: Continuation<R>,
    internal val wasmContBox: WasmContinuationBox =
        WasmContinuationBox(nullableContrefIntrinsic(), false)
) : CoroutineImpl<T, R>(resultContinuation) {

    protected val _resultContinuation = resultContinuation
    override val _context: CoroutineContext = resultContinuation.context

    @Suppress("UNCHECKED_CAST")
    override fun resumeWith(result: Result<T>) {
        this.result = result.getOrNull()
        exception = result.exceptionOrNull()

        if (wasmContBox.pendingSuspend) {
            wasmContBox.pendingSuspend = false
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

        releaseIntercepted() // this instance is terminating

        val completion = _resultContinuation

        // top-level completion reached -- invoke and return
        if (exception != null) {
            completion.resumeWithException(exception!!)
        } else {
            completion.resume(this.result as R)
        }
    }

    override fun doResume(): Any? {
        val wasmCont = wasmContBox.wasmContinuation!!

        val resumeResult: Any? = exception?.let {
            resumeThrowImpl(it, wasmCont)
        } ?: resumeWithImpl(_resultContinuation, wasmCont)

        return resumeResult
    }
}

internal class WasmContinuationBox @WasmPrimitiveConstructor constructor(
    var wasmContinuation: contref1?,
    var pendingSuspend: Boolean
)
