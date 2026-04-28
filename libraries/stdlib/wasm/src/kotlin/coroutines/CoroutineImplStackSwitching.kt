/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:WasmStackSwitchingOnly

package kotlin.coroutines

import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.internal.UsedFromCompilerGeneratedCode
import kotlin.wasm.internal.WasmPrimitiveConstructor
import kotlin.wasm.internal.WasmStackSwitchingOnly
import kotlin.wasm.internal.nullableContrefIntrinsic
import kotlin.wasm.internal.reftypes.contref1
import kotlin.wasm.internal.resumeThrowImpl
import kotlin.wasm.internal.resumeWithImpl


@SinceKotlin("1.3")
@UsedFromCompilerGeneratedCode
internal abstract class CoroutineImplStackSwitching<T, R>(
    resultContinuation: Continuation<R>,
    val rethrowExceptions: Boolean = false
) : CoroutineImpl<T, R>(resultContinuation) {

    internal var wasSuspended = false

    protected val _resultContinuation = resultContinuation
    override val _context: CoroutineContext = resultContinuation.context

    @Suppress("UNCHECKED_CAST")
    override fun resumeWith(result: Result<T>) {
        this.result = result.getOrNull()
        exception = result.exceptionOrNull()

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
            if (rethrowExceptions && !wasSuspended) throw exception!!
            completion.resumeWithException(exception!!)
        } else {
            if (rethrowExceptions && !wasSuspended) return // prevent double-completion
            completion.resume(this.result as R)
        }
        return
    }
}

internal class WasmContinuationBox @WasmPrimitiveConstructor constructor(
    var wasmContinuation: contref1?,
    var pendingSuspend: Boolean
)

internal class WasmContinuation<T, R>(
    internal val wasmContBox: WasmContinuationBox,
    completion: Continuation<R>,
    rethrowExceptions: Boolean = false
) : CoroutineImplStackSwitching<T, R>(completion, rethrowExceptions) {

    override fun resumeWith(result: Result<T>) {
        // Handle synchronous resume inside user's block
        if (wasmContBox.pendingSuspend) {
            this.result = result.getOrNull()
            this.exception = result.exceptionOrNull()
            wasmContBox.pendingSuspend = false
            return
        }
        super.resumeWith(result)
    }

    override fun doResume(): Any? {
        val wasmCont = wasmContBox.wasmContinuation ?: run {
            val e = exception
            if (e != null) throw e
            return result
        }
        wasmContBox.wasmContinuation = nullableContrefIntrinsic()  // consume before calling resume to prevent re-use

        val resumeResult: Any? = exception?.let {
            resumeThrowImpl(it, wasmCont)
        } ?: resumeWithImpl(this, wasmCont)

        if (resumeResult === COROUTINE_SUSPENDED) {
            wasSuspended = true
            return COROUTINE_SUSPENDED
        }
        return resumeResult
    }
}
