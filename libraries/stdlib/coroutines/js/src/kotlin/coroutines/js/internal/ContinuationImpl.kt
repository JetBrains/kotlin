/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.coroutines

import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED

@SinceKotlin("1.3")
@JsName("CoroutineImpl")
internal abstract class CoroutineImpl(private val resultContinuation: Continuation<Any?>) : Continuation<Any?> {
    protected var state = 0
    protected var exceptionState = 0
    protected var result: Any? = null
    protected var exception: Throwable? = null
    protected var finallyPath: Array<Int>? = null

    public override val context: CoroutineContext = resultContinuation.context

    private var intercepted_: Continuation<Any?>? = null

    public fun intercepted(): Continuation<Any?> =
        intercepted_
                ?: (context[ContinuationInterceptor]?.interceptContinuation(this) ?: this)
                    .also { intercepted_ = it }

    override fun resumeWith(result: SuccessOrFailure<Any?>) {
        if (result.isSuccess) {
            this.result = result.value
        } else {
            state = exceptionState
            this.exception = result.exception
        }
        try {
            val resumeResult = doResume()
            if (resumeResult !== COROUTINE_SUSPENDED) {
                resultContinuation.resume(resumeResult)
            }
        } catch (t: Throwable) {
            resultContinuation.resumeWithException(t)
        }
    }

    protected abstract fun doResume(): Any?
}