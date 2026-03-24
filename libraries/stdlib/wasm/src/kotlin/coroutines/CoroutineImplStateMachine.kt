/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.coroutines

import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.internal.UsedFromCompilerGeneratedCode


@SinceKotlin("1.3")
@UsedFromCompilerGeneratedCode
internal abstract class CoroutineImplStateMachine(private val resultContinuation: Continuation<Any?>?) : CoroutineImpl<Any?>() {

    override val _context: CoroutineContext? = resultContinuation?.context

    override fun resumeWith(result: Result<Any?>) {
        var current = this
        var currentResult: Any? = result.getOrNull()
        var currentException: Throwable? = result.exceptionOrNull()

        // This loop unrolls recursion in current.resumeWith(param) to make saner and shorter stack traces on resume
        while (true) {
            with(current) {
                // Set result and exception fields in the current continuation
                if (currentException == null) {
                    this.result = currentResult
                } else {
                    state = exceptionState
                    exception = currentException
                }

                try {
                    val outcome = doResume()
                    if (outcome === COROUTINE_SUSPENDED) return
                    currentResult = outcome
                    currentException = null
                } catch (exception: Throwable) { // Catch all exceptions
                    currentResult = null
                    currentException = exception
                }

                releaseIntercepted() // this state machine instance is terminating

                val completion = resultContinuation!!

                if (completion is CoroutineImplStateMachine) {
                    // unrolling recursion via loop
                    current = completion
                } else {
                    // top-level completion reached -- invoke and return
                    if (currentException != null) {
                        completion.resumeWithException(currentException)
                    } else {
                        completion.resume(currentResult)
                    }
                    return
                }
            }
        }
    }
}
