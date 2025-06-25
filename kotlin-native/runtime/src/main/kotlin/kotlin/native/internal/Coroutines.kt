/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.native.internal

import kotlin.coroutines.*
import kotlin.native.internal.escapeAnalysis.Escapes
import kotlin.internal.getContinuation

@kotlin.internal.InlineOnly
@PublishedApi
internal inline suspend fun getCoroutineContext(): CoroutineContext =
        getContinuation<Any?>().context

@TypedIntrinsic(IntrinsicType.SAVE_COROUTINE_STATE)
@PublishedApi
internal external fun saveCoroutineState()

@TypedIntrinsic(IntrinsicType.RESTORE_COROUTINE_STATE)
@PublishedApi
internal external fun restoreCoroutineState()

