/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:WasmCoroutineMode(isStackSwitchingMode = false)

package kotlin.wasm.internal

import kotlin.coroutines.Continuation
import kotlin.internal.UsedFromCompilerGeneratedCode

// Is replaced by Stack Switching intrinsic when -Xwasm-use-stack-switching-proposal passed
@PublishedApi
@UsedFromCompilerGeneratedCode
internal suspend fun <T> suspendCoroutineUninterceptedOrReturnIntrinsic(block: (Continuation<T>) -> Any?): T =
    returnIfSuspended<T>(block(getContinuation<T>()))
