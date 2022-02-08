/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("RedundantSuspendModifier")

package kotlin.wasm.internal

import kotlin.coroutines.*

@PublishedApi
@ExcludedFromCodegen
internal fun <T> getContinuation(): Continuation<T> =
    implementedAsIntrinsic

@PublishedApi
@Suppress("UNCHECKED_CAST")
internal suspend fun <T> returnIfSuspended(@Suppress("UNUSED_PARAMETER") argument: Any?): T =
    argument as T

@PublishedApi
internal fun <T> interceptContinuationIfNeeded(
    context: CoroutineContext,
    continuation: Continuation<T>
) = context[ContinuationInterceptor]?.interceptContinuation(continuation) ?: continuation


@PublishedApi
internal suspend fun getCoroutineContext(): CoroutineContext = getContinuation<Any?>().context

@PublishedApi
internal suspend fun <T> suspendCoroutineUninterceptedOrReturn(block: (Continuation<T>) -> Any?): T =
    returnIfSuspended<T>(block(getContinuation<T>()))

@ExcludedFromCodegen
@PublishedApi
internal fun <T> startCoroutineUninterceptedOrReturnIntrinsic0(
    f: (suspend () -> T),
    completion: Continuation<T>
): Any? {
    implementedAsIntrinsic
}

@ExcludedFromCodegen
@PublishedApi
internal fun <R, T> startCoroutineUninterceptedOrReturnIntrinsic1(
    f: (suspend R.() -> T),
    receiver: R,
    completion: Continuation<T>
): Any? {
    implementedAsIntrinsic
}

@ExcludedFromCodegen
@PublishedApi
internal fun <R, P, T> startCoroutineUninterceptedOrReturnIntrinsic2(
    f: (suspend R.(P) -> T),
    receiver: R,
    param: P,
    completion: Continuation<T>
): Any? {
    implementedAsIntrinsic
}

@PublishedApi
@SinceKotlin("1.3")
internal val EmptyContinuation = Continuation<Any?>(EmptyCoroutineContext) { result ->
    result.getOrThrow()
}