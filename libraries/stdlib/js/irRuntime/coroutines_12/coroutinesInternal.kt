/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.coroutines.experimental

internal abstract class CoroutineImpl(private val completion: Continuation<Any?>) : Continuation<Any?> {
    protected var exceptionState = 0
    protected var state: Int = 0

    protected var exception: dynamic = null
    protected var result: dynamic = null

    public override val context: CoroutineContext get() = completion?.context

    val facade: Continuation<Any?> get() {
        return if (context != null) interceptContinuationIfNeeded(context, this)
        else this
    }

    override fun resume(value: Any?) {
        this.result = value
        doResumeWrapper()
    }

    override fun resumeWithException(exception: Throwable) {
        // TODO: once we have arrays working refact it with exception table
        this.state = exceptionState
        this.exception = exception
        doResumeWrapper()
    }

    protected fun doResumeWrapper() {
        processBareContinuationResume(completion) { doResume() }
    }

    protected abstract fun doResume(): Any?

    open fun create(completion: Continuation<*>): Continuation<Unit> {
        throw IllegalStateException("create(Continuation) has not been overridden")
    }

    open fun create(value: Any?, completion: Continuation<*>): Continuation<Unit> {
        throw IllegalStateException("create(Any?;Continuation) has not been overridden")
    }
}