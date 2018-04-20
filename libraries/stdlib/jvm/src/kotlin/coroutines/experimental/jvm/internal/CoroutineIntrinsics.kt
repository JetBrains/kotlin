/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("CoroutineIntrinsics")

package kotlin.coroutines.experimental.jvm.internal

import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.ContinuationInterceptor
import kotlin.coroutines.experimental.CoroutineContext

/**
 * @suppress
 */
fun <T> normalizeContinuation(continuation: Continuation<T>): Continuation<T> =
    (continuation as? CoroutineImpl)?.facade ?: continuation

internal fun <T> interceptContinuationIfNeeded(
    context: CoroutineContext,
    continuation: Continuation<T>
) = context[ContinuationInterceptor]?.interceptContinuation(continuation) ?: continuation
