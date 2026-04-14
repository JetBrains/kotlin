/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.wasm.internal

import kotlin.coroutines.Continuation
import kotlin.internal.DoNotInlineOnFirstStage
import kotlin.internal.UsedFromCompilerGeneratedCode

// Is replaced by Stack Switching implementation when -Xwasm-coroutines-stack-switching passed
@PublishedApi
@DoNotInlineOnFirstStage
@UsedFromCompilerGeneratedCode
internal suspend inline fun <T> suspendCoroutineUninterceptedOrReturn(block: (Continuation<T>) -> Any?): T =
    returnIfSuspended<T>(block(getContinuation<T>()))
