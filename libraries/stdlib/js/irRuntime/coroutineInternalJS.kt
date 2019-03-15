/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */


package kotlin.js

import kotlin.js.coroutineAliases.*


@PublishedApi
internal fun <T> getContinuation(): ContinuationAlias<T> { throw Exception("Implemented as intrinsic") }
// Do we really need this intrinsic in JS?

@PublishedApi
internal suspend fun <T> returnIfSuspended(@Suppress("UNUSED_PARAMETER") argument: Any?): T {
    throw Exception("Implemented as intrinsic")
}

@PublishedApi
internal fun <T> interceptContinuationIfNeeded(
    context: CoroutineContextAlias,
    continuation: ContinuationAlias<T>
) = context[ContinuationInterceptorAlias]?.interceptContinuation(continuation) ?: continuation


@SinceKotlin("1.2")
@PublishedApi
internal suspend fun getCoroutineContext(): CoroutineContextAlias = getContinuation<Any?>().context

// TODO: remove `JS` suffix oncec `NameGenerator` is implemented
@PublishedApi
internal suspend fun <T> suspendCoroutineUninterceptedOrReturnJS(block: (ContinuationAlias<T>) -> Any?): T =
    returnIfSuspended<T>(block(getContinuation<T>()))


