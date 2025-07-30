/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.internal

import kotlin.coroutines.Continuation
import kotlin.native.internal.IntrinsicType
import kotlin.native.internal.TypedIntrinsic

@PublishedApi
@TypedIntrinsic(IntrinsicType.GET_CONTINUATION)
internal actual external fun <T> getContinuation(): Continuation<T>

@TypedIntrinsic(IntrinsicType.RETURN_IF_SUSPENDED)
@PublishedApi
internal actual external suspend fun <T> returnIfSuspended(@Suppress("UNUSED_PARAMETER") argument: Any?): T