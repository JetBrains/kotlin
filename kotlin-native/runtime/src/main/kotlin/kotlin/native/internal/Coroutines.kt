/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.native.internal

import kotlin.coroutines.*
import kotlin.internal.UsedFromCompilerGeneratedCode
import kotlin.native.internal.escapeAnalysis.Escapes

@kotlin.internal.InlineOnly
@UsedFromCompilerGeneratedCode
@PublishedApi
internal inline suspend fun <T> suspendCoroutineUninterceptedOrReturn(crossinline block: (Continuation<T>) -> Any?): T =
        returnIfSuspended<T>(block(getContinuation<T>()))

@TypedIntrinsic(IntrinsicType.GET_CONTINUATION)
@PublishedApi
@UsedFromCompilerGeneratedCode
internal external fun <T> getContinuation(): Continuation<T>

@kotlin.internal.InlineOnly
@UsedFromCompilerGeneratedCode
@PublishedApi
internal inline suspend fun getCoroutineContext(): CoroutineContext =
        getContinuation<Any?>().context

@TypedIntrinsic(IntrinsicType.RETURN_IF_SUSPENDED)
@PublishedApi
@UsedFromCompilerGeneratedCode
internal external suspend fun <T> returnIfSuspended(@Suppress("UNUSED_PARAMETER") argument: Any?): T

@TypedIntrinsic(IntrinsicType.SAVE_COROUTINE_STATE)
@PublishedApi
@UsedFromCompilerGeneratedCode
internal external fun saveCoroutineState()

@TypedIntrinsic(IntrinsicType.RESTORE_COROUTINE_STATE)
@PublishedApi
@UsedFromCompilerGeneratedCode
internal external fun restoreCoroutineState()

