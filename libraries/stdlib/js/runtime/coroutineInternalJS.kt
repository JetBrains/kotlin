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
@Suppress("UNCHECKED_CAST")
internal suspend fun <T> returnIfSuspended(value: Any?): T {
    return value as T
}

@PublishedApi
@UsedFromCompilerGeneratedCode
@Suppress("UNCHECKED_CAST")
internal suspend inline fun <T> returnIfSuspendedNonGeneratorVersion(block: () -> Any?): T {
    return block().unsafeCast<T>()
}

// TODO: remove `JS` suffix oncec `NameGenerator` is implemented
@UsedFromCompilerGeneratedCode
@PublishedApi
internal suspend inline fun <T> suspendCoroutineUninterceptedOrReturnJS(block: (Continuation<T>) -> Any?): T {
    val continuation = getContinuation<T>()
    return returnIfSuspendedNonGeneratorVersion { block(continuation) }
}
