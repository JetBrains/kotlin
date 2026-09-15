/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:WasmCoroutineMode(isStackSwitchingMode = false)

package kotlin.wasm.internal

import kotlin.coroutines.Continuation
import kotlin.internal.UsedFromCompilerGeneratedCode

// Are replaced with Stack Switching intrinsics when -Xwasm-use-stack-switching-proposal passed.
@Suppress("UNUSED_PARAMETER")
@PublishedApi
internal suspend fun <T> getBlockKotlinContinuation(): Continuation<T> =
    getContinuation<T>()

@Suppress("UNUSED_PARAMETER", "UNCHECKED_CAST")
@PublishedApi
internal suspend fun <T> processSuspendBlockResult(result: Any?, blockContinuation: Continuation<T>): T =
    result as T
