/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.coroutines.experimental

/**
 * Marks coroutine context element that intercepts coroutine continuations.
 * The coroutines framework uses [ContinuationInterceptor.Key] to retrieve the interceptor and
 * intercepts all coroutine continuations with [interceptContinuation] invocations.
 */
@SinceKotlin("1.1")
public interface ContinuationInterceptor : CoroutineContext.Element {
    /**
     * The key that defines *the* context interceptor.
     */
    companion object Key : CoroutineContext.Key<ContinuationInterceptor>

    /**
     * Returns continuation that wraps the original [continuation], thus intercepting all resumptions.
     * This function is invoked by coroutines framework when needed and the resulting continuations are
     * cached internally per each instance of the original [continuation].
     *
     * By convention, implementations that install themselves as *the* interceptor in the context with
     * the [Key] shall also scan the context for other element that implement [ContinuationInterceptor] interface
     * and use their [interceptContinuation] functions, too.
     */
    public fun <T> interceptContinuation(continuation: Continuation<T>): Continuation<T>
}
