/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */


package kotlin.js

import kotlin.coroutines.*


@PublishedApi
internal fun <T> getContinuation(): Continuation<T> { throw Exception("Implemented as intrinsic") }
// Do we really need this intrinsic in JS?

@PublishedApi
@Suppress("UNCHECKED_CAST")
internal suspend fun <T> returnIfSuspended(argument: Any?): T {
    return argument as T
}

@PublishedApi
internal fun <T> interceptContinuationIfNeeded(
    context: CoroutineContext,
    continuation: Continuation<T>
): Continuation<T> = context[ContinuationInterceptor]?.interceptContinuation(continuation) ?: continuation


@SinceKotlin("1.2")
@PublishedApi
internal inline suspend fun getCoroutineContext(): CoroutineContext = getContinuation<Any?>().context

// TODO: remove `JS` suffix oncec `NameGenerator` is implemented
@PublishedApi
internal inline suspend fun <T> suspendCoroutineUninterceptedOrReturnJS(block: (Continuation<T>) -> Any?): T =
    returnIfSuspended<T>(block(getContinuation<T>()))


