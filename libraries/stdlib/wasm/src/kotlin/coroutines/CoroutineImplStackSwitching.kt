/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.coroutines

// TODO (Stack Switching): uncomment stack switching coroutine implementation

//import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
//import kotlin.internal.UsedFromCompilerGeneratedCode
//import kotlin.wasm.internal.ResumeIntrinsicResult
//import kotlin.wasm.internal.WasmStackSwitchingOnly
//import kotlin.wasm.internal.reftypes.contref1
//import kotlin.wasm.internal.resumeThrowImpl
//import kotlin.wasm.internal.resumeWithImpl
//
//
//@SinceKotlin("1.3")
//@UsedFromCompilerGeneratedCode
//internal abstract class CoroutineImplStackSwitching<T, R>(
//    resultContinuation: Continuation<R>,
//    val rethrowExceptions: Boolean = false
//) : CoroutineImpl<T, R>(resultContinuation) {
//
//    internal var wasSuspended = false
//
//    protected val _resultContinuation = resultContinuation
//    override val _context: CoroutineContext = resultContinuation.context
//
//    @Suppress("UNCHECKED_CAST")
//    override fun resumeWith(result: Result<T>) {
//        this.result = result.getOrNull()
//        exception = result.exceptionOrNull()
//
//        if (exception != null) {
//            state = exceptionState
//        }
//
//        try {
//            val outcome = doResume()
//            this.result = outcome
//            exception = null
//            if (outcome === COROUTINE_SUSPENDED) return
//        } catch (exception: Throwable) { // Catch all exceptions
//            this.result = null
//            this.exception = exception
//        }
//
//        releaseIntercepted() // this instance is terminating
//
//        val completion = _resultContinuation
//
//        // top-level completion reached -- invoke and return
//        if (exception != null) {
//            if (rethrowExceptions && !wasSuspended) throw exception!!
//            completion.resumeWithException(exception!!)
//        } else {
//            if (rethrowExceptions ) return
//            completion.resume(this.result as R)
//        }
//        return
//    }
//}
//
//@WasmStackSwitchingOnly
//internal class WasmContinuation<T, R>(
//    internal var wasmContBox: contref1,
//    completion: Continuation<R>,
//    rethrowExceptions: Boolean = false
//) : CoroutineImplStackSwitching<T, R>(completion, rethrowExceptions) {
//
//    private var isResumed = false
//    private var isFreshInstance = true
//    override fun doResume(): Any? {
//        do {
//            require(!isResumed) { "WasmContinuation can be resumed only once" }
//            isResumed = true
//
//            val resultValue = if (isFreshInstance && exception == null) {
//                require(result == Unit || result == _resultContinuation)
//                isFreshInstance = false
//                _resultContinuation
//            } else result
//            val resumeResult: ResumeIntrinsicResult = exception?.let {
//                resumeThrowImpl(it, wasmContBox)
//            } ?: resumeWithImpl(resultValue, wasmContBox)
//
//            wasmContBox = resumeResult.remainingFunction ?: return resumeResult.result
//            isResumed = false
//            wasSuspended = true
//            if (resumeResult.result === COROUTINE_SUSPENDED) return COROUTINE_SUSPENDED
//            require(resumeResult.suspendBody != null)
//            val suspendBodyResult = try {
//                resumeResult.suspendBody(this).takeIf { it !== COROUTINE_SUSPENDED }?.let { Result.success(it) }
//            } catch (e: Throwable) {
//                Result.failure(e)
//            }
//            if (suspendBodyResult == null) {
//                return COROUTINE_SUSPENDED
//            }
//            result = suspendBodyResult.getOrNull()
//            exception = suspendBodyResult.exceptionOrNull()
//        } while (true)
//    }
//}
