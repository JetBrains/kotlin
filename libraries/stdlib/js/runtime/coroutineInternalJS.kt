/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */


package kotlin.js

import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.internal.UsedFromCompilerGeneratedCode


@PublishedApi
internal fun <T> getContinuation(): Continuation<T> { throw Exception("Implemented as intrinsic") }
// Do we really need this intrinsic in JS?

@PublishedApi
internal fun <T> interceptContinuationIfNeeded(
    context: CoroutineContext,
    continuation: Continuation<T>
): Continuation<T> = context[ContinuationInterceptor]?.interceptContinuation(continuation) ?: continuation


@SinceKotlin("1.2")
@UsedFromCompilerGeneratedCode
@PublishedApi
internal inline suspend fun getCoroutineContext(): CoroutineContext = getContinuation<Any?>().context

@PublishedApi
internal class WrapperContinuation<T>(private val resultContinuation: Continuation<T>) : Continuation<T> {
    override val context: CoroutineContext
        get() = resultContinuation.context

    var shouldResumeWith: Result<T>? = null
    var wasCalledInsideSuspendOrReturn = true

    override fun resumeWith(result: Result<T>) {
        if (wasCalledInsideSuspendOrReturn) {
            require(shouldResumeWith == null) { "Attempt to resume continuation twice" }
            shouldResumeWith = result
        } else {
            resultContinuation.resumeWith(result)
        }
    }
}

@OptIn(JsIntrinsic::class)
@Suppress("UNCHECKED_CAST")
@PublishedApi
internal suspend fun <T> suspendOrReturn(argument: Any?, wrapperContinuation: WrapperContinuation<T>): T {
    var result = argument
    val shouldResumeWith = wrapperContinuation.shouldResumeWith

    if (shouldResumeWith == null) {
        wrapperContinuation.wasCalledInsideSuspendOrReturn = false
        if (argument === COROUTINE_SUSPENDED) result = jsYield(argument)
    } else {
        val exception = shouldResumeWith.exceptionOrNull()
        if (exception != null) throw exception
        result = shouldResumeWith.value
    }

    return result as T
}

@PublishedApi
@Suppress("UNCHECKED_CAST")
internal suspend fun <T> returnIfSuspended(argument: Any?): T {
    return argument as T
}

// TODO: remove `JS` suffix oncec `NameGenerator` is implemented
@UsedFromCompilerGeneratedCode
@PublishedApi
@Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
internal suspend inline fun <T> suspendCoroutineUninterceptedOrReturnJS(block: (Continuation<T>) -> Any?): T {
    val dummy = WrapperContinuation<T>(getContinuation<T>())
    return suspendOrReturn(block(dummy), dummy)
}
