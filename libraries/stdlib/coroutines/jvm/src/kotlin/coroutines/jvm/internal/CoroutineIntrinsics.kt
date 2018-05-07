/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("CoroutineIntrinsics")

package kotlin.coroutines.jvm.internal

import kotlin.coroutines.Continuation
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext

/**
 * @suppress
 */
@SinceKotlin("1.3")
public fun <T> normalizeContinuation(continuation: Continuation<T>): Continuation<T> =
    (continuation as? CoroutineImpl)?.facade ?: continuation

@SinceKotlin("1.3")
internal fun <T> interceptContinuationIfNeeded(
    context: CoroutineContext,
    continuation: Continuation<T>
) = context[ContinuationInterceptor]?.interceptContinuation(continuation) ?: continuation
