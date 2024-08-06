/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.coroutines

internal abstract class InterceptedCoroutine : Continuation<Any?> {

    @Suppress("PropertyName")
    abstract var _intercepted: Continuation<Any?>?

    fun intercepted(): Continuation<Any?> =
        _intercepted
            ?: (context[ContinuationInterceptor]?.interceptContinuation(this) ?: this)
                .also { _intercepted = it }

    protected open fun releaseIntercepted() {
        val intercepted = _intercepted
        if (intercepted != null && intercepted !== this) {
            context[ContinuationInterceptor]!!.releaseInterceptedContinuation(intercepted)
        }
        this._intercepted = CompletedContinuation
    }
}
