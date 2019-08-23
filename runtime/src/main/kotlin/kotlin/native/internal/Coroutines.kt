/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.native.internal

import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

@kotlin.internal.InlineOnly
@PublishedApi
internal inline suspend fun <T> suspendCoroutineUninterceptedOrReturn(crossinline block: (Continuation<T>) -> Any?): T =
        returnIfSuspended<T>(block(getContinuation<T>()))

@TypedIntrinsic(IntrinsicType.GET_CONTINUATION)
@PublishedApi
internal external fun <T> getContinuation(): Continuation<T>

@kotlin.internal.InlineOnly
@PublishedApi
internal inline suspend fun getCoroutineContext(): CoroutineContext =
        getContinuation<Any?>().context

@TypedIntrinsic(IntrinsicType.RETURN_IF_SUSPENDED)
@PublishedApi
internal external suspend fun <T> returnIfSuspended(@Suppress("UNUSED_PARAMETER") argument: Any?): T

@TypedIntrinsic(IntrinsicType.COROUTINE_LAUNCHPAD)
internal external fun coroutineLaunchpad(suspendFunctionCall: Any?, continuation: Continuation<*>): Any?