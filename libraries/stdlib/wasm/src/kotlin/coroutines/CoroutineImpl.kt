/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.coroutines

import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.wasm.internal.SuspensionMarker

@SinceKotlin("1.3")
internal abstract class CoroutineImpl<T, R>(internal val resultContinuation: Continuation<R>, val rethrowExceptions: Boolean = false) : Continuation<T> {
    protected var state = 0
    protected var exceptionState = 0
    internal var result: Any? = null
    protected var exception: Throwable? = null
    protected var finallyPath: Array<Int>? = null
    internal var wasSuspended = false

    private val _context: CoroutineContext = resultContinuation.context

    public override val context: CoroutineContext get() = _context

    private var intercepted_: Continuation<T>? = null

    public fun intercepted(): Continuation<T> = intercepted_
        ?: (context[ContinuationInterceptor]?.interceptContinuation(this) ?: this)
            .also { intercepted_ = it }

    @Suppress("UNCHECKED_CAST")
    override fun resumeWith(result: Result<T>) {
        this.result = result.getOrNull()
        exception = result.exceptionOrNull()

        if (exception != null) {
            state = exceptionState
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

        releaseIntercepted() // this state machine instance is terminating

        val completion = resultContinuation

        // top-level completion reached -- invoke and return
        val wasSuspendedInMarker = completion.context[SuspensionMarker]?.wasSuspended == true
        val wasReallySuspended = wasSuspended || wasSuspendedInMarker
        if (exception != null) {
            if (rethrowExceptions && !wasReallySuspended) throw exception!!
            completion.resumeWithException(exception!!)
        } else {
            if (rethrowExceptions && !wasReallySuspended) return // prevent double-completion
            completion.resume(this.result as R)
        }
        return
    }

    private fun releaseIntercepted() {
        val intercepted = intercepted_
        if (intercepted != null && intercepted !== this) {
            context[ContinuationInterceptor]!!.releaseInterceptedContinuation(intercepted)
        }
        this.intercepted_ = CompletedContinuation // just in case
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
