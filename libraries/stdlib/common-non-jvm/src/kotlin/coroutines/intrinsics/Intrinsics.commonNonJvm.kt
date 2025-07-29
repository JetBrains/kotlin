/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.coroutines.intrinsics

import kotlin.coroutines.Continuation
import kotlin.internal.InlineOnly
import kotlin.internal.getContinuation
import kotlin.internal.returnIfSuspended

@PublishedApi
@InlineOnly
internal actual suspend inline fun <T> suspendCoroutineUninterceptedOrReturnImpl(crossinline block: (Continuation<T>) -> Any?): T {
    return returnIfSuspended(block(getContinuation()))
}