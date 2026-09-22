/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:WasmCoroutineMode(isStackSwitchingMode = false)

package kotlin.wasm.internal

import kotlin.internal.UsedFromCompilerGeneratedCode

// Is replaced by Stack Switching intrinsic when -Xwasm-use-stack-switching-proposal passed
@Suppress("UNCHECKED_CAST", "RedundantSuspendModifier")
@PublishedApi
@UsedFromCompilerGeneratedCode
internal suspend fun <T> suspendOrReturn(result: Any?): T =
    result as T
