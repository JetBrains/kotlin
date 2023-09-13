/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.coroutines

import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED

@SinceKotlin("1.3")
@JsName("CoroutineImpl")
internal abstract class CoroutineImpl(
    private val resultContinuation: Continuation<Any?>?
) : InterceptedCoroutine(), Continuation<Any?> {
    protected var state = 0
    protected var exceptionState = 0
    protected var result: dynamic = null
    protected var exception: dynamic = null
    protected var finallyPath: Array<Int>? = null

    private val _context: CoroutineContext? = resultContinuation?.context

    public override val context: CoroutineContext get() = _context!!

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
                } catch (exception: dynamic) { // Catch all exceptions
                    currentResult = null
                    currentException = exception.unsafeCast<Throwable>()
                }

                releaseIntercepted() // this state machine instance is terminating

                val completion = resultContinuation!!

                if (completion is CoroutineImpl) {
                    // unrolling recursion via loop
                    current = completion
                } else {
                    // top-level completion reached -- invoke and return
                    if (currentException != null) {
                        completion.resumeWithException(currentException!!)
                    } else {
                        completion.resume(currentResult)
                    }
                    return
                }
            }
        }
    }

    protected abstract fun doResume(): Any?

    public open fun create(completion: Continuation<*>): Continuation<Unit> {
        throw UnsupportedOperationException("create(Continuation) has not been overridden")
    }

    public open fun create(value: Any?, completion: Continuation<*>): Continuation<Unit> {
        throw UnsupportedOperationException("create(Any?;Continuation) has not been overridden")
    }
}

internal object CompletedContinuation : Continuation<Any?> {
    override val context: CoroutineContext
        get() = error("This continuation is already complete")

    override fun resumeWith(result: Result<Any?>) {
        error("This continuation is already complete")
    }

    override fun toString(): String = "This continuation is already complete"
}
